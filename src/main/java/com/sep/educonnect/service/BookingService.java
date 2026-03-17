package com.sep.educonnect.service;

import com.sep.educonnect.dto.booking.*;
import com.sep.educonnect.dto.booking.admin.BookingAdminDetailResponse;
import com.sep.educonnect.dto.booking.admin.BookingAdminListItemResponse;
import com.sep.educonnect.dto.notification.request.NotificationUpdateRequest;
import com.sep.educonnect.dto.tutor.request.WeeklySchedule;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
  private static final String[] DAY_OF_WEEK_LABELS = {
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
  };
  private static final DateTimeFormatter SLOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final String PAYMENT_GATEWAY_PAYOS = "PayOS";
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final BookingMemberRepository bookingMemberRepository;
  private final TutorClassRepository tutorClassRepository;
  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final ClassSessionRepository classSessionRepository;
  private final NotificationService notificationService;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request) {
    // Resolve current user from security context
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    Course course =
        courseRepository
            .findByIdAndIsDeletedFalse(request.getCourseId())
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

    if (course.getStatus() == CourseStatus.INACTIVE) {
      throw new AppException(ErrorCode.COURSE_INACTIVE);
    }

    if (bookingMemberRepository.existsByUserIdAndBooking_Course_Id(
        currentUser.getUserId(), course.getId())) {
      throw new AppException(ErrorCode.BOOKING_ALREADY_EXISTS);
    }

    boolean isSelfPaced = CourseType.SELF_PACED.equals(course.getType());

    Set<String> memberIds = resolveMemberIds(request, currentUser, isSelfPaced);
    int numberOfMembers = memberIds.size();

    // Check for schedule overlap if not self-paced and has weekly schedules
    if (!isSelfPaced
        && request.getWeeklySchedules() != null
        && !request.getWeeklySchedules().isEmpty()) {
      String overlapCheck =
          checkScheduleOverlap(memberIds, null, null, null, request.getWeeklySchedules(), null);
      if (!"OK".equals(overlapCheck)) {
        throw new AppException(ErrorCode.OVERLAP_STUDENT_SCHEDULE);
      }
    }

    BigDecimal totalAmount = course.getPrice().multiply(BigDecimal.valueOf(numberOfMembers));
    ;
    String scheduleDescription =
        isSelfPaced
            ? null
            : buildScheduleDescription(request.getWeeklySchedules(), request.getLessons());

    Booking booking =
        Booking.builder()
            .course(course)
            .groupType(request.getGroupType())
            .registrationType(request.getRegistrationType())
            .bookingStatus(isSelfPaced ? BookingStatus.APPROVED : BookingStatus.PENDING)
            .totalAmount(totalAmount)
            .scheduleDescription(scheduleDescription)
            .build();

    attachBookingMembers(booking, memberIds, currentUser.getUserId());

    Booking savedBooking = bookingRepository.save(booking);

    // Send notifications
    String courseName = course.getName();
    String tutorName =
        course.getTutor() != null
            ? course.getTutor().getFirstName() + " " + course.getTutor().getLastName()
            : "the tutor";

    // Send TYPICAL notification to owner
    notificationService.createAndSendNotification(
        currentUser.getUserId(),
        "You have booked " + tutorName + " for course " + courseName + ", waiting for approval",
        NotificationType.TYPICAL,
        currentUser.getAvatar(),
        null);

    // Batch fetch all members for notifications and response mapping
    List<String> memberIdsList = new ArrayList<>(memberIds);
    List<User> members = userRepository.findAllById(memberIdsList);
    Map<String, User> userMap = new HashMap<>();
    userMap.put(currentUser.getUserId(), currentUser); // Add current user
    for (User member : members) {
      userMap.put(member.getUserId(), member);
    }

    // Send BOOKING_INVITE notifications to other members
    String actionLink =
        NotificationLink.STUDENT_BOOKING_INVITATION.getPrefix() + "/" + savedBooking.getId();
    for (String memberId : memberIds) {
      if (!memberId.equals(currentUser.getUserId())) {
        User member = userMap.get(memberId);
        if (member != null) {
          String memberName = member.getFirstName() + " " + member.getLastName();
          notificationService.createAndSendNotification(
              memberId,
              memberName
                  + " has invited you to study with "
                  + tutorName
                  + " for course "
                  + courseName,
              NotificationType.BOOKING_INVITE,
              null,
              actionLink);
        } else {
          log.warn("Cannot send notification to member {}: user not found", memberId);
        }
      }
    }

    return toBookingResponse(savedBooking, userMap);
  }

  @Transactional
  public BookingResponse joinClass(JoinClassRequest request) {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    TutorClass tutorClass =
        tutorClassRepository
            .findByIdAndIsDeletedFalse(request.getClassId())
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

    if (tutorClass.getLastJoinDate() != null
        && tutorClass.getLastJoinDate().isBefore(LocalDate.now())) {
      throw new AppException(ErrorCode.CLASS_JOIN_DEADLINE_PASSED);
    }

    // Check for schedule overlap with student's existing classes
    String overlapCheck =
        checkScheduleOverlap(
            Collections.singletonList(currentUser.getUserId()),
            null,
            null,
            tutorClass.getId(),
            null,
            null);
    if (!"OK".equals(overlapCheck)) {
      throw new AppException(ErrorCode.OVERLAP_STUDENT_SCHEDULE);
    }

    Course course = tutorClass.getCourse();
    BigDecimal totalAmount =
        (course != null && course.getPrice() != null) ? course.getPrice() : BigDecimal.ZERO;

    Booking booking =
        Booking.builder()
            .course(course)
            .groupType(GroupType.INDIVIDUAL)
            .registrationType(RegistrationType.REGULAR)
            .bookingStatus(BookingStatus.PENDING)
            .totalAmount(totalAmount)
            .scheduleDescription(
                "Join Class:" + tutorClass.getId() + " - " + tutorClass.getTitle() + " request")
            .build();

    attachBookingMembers(
        booking, Collections.singleton(currentUser.getUserId()), currentUser.getUserId());
    Booking savedBooking = bookingRepository.save(booking);

    // Build user map (only current user in this case)
    Map<String, User> userMap = new HashMap<>();
    userMap.put(currentUser.getUserId(), currentUser);

    // Send TYPICAL notification to owner
    String courseName = tutorClass.getCourse() != null ? tutorClass.getCourse().getName() : "";
    notificationService.createAndSendNotification(
        currentUser.getUserId(),
        "You have joined class " + tutorClass.getTitle() + " for course " + courseName,
        NotificationType.TYPICAL,
        null,
        null);

    return toBookingResponse(savedBooking, userMap);
  }

  public BookingStateResponse getBookingState(Long courseId) {
    // Check if user is authenticated
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || "anonymousUser".equals(authentication.getName())) {
      // User is not logged in - return default state for guest users
      return new BookingStateResponse(false, null, List.of());
    }

    String currentUsername = authentication.getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    if (courseRepository.findByIdAndIsDeletedFalse(courseId).isEmpty()) {
      throw new AppException(ErrorCode.COURSE_NOT_EXISTED);
    }

    // Check if already joined via booking (excluding REJECTED bookings)
    boolean hasBooking =
        bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
            currentUser.getUserId(), courseId);

    // Check if enrolled in class
    Optional<ClassEnrollment> enrollmentOpt =
        classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id(
            currentUser.getUserId(), courseId);

    boolean alreadyJoined = hasBooking || enrollmentOpt.isPresent();
    Long joinedClassId = enrollmentOpt.map(e -> e.getTutorClass().getId()).orElse(null);

    // If already joined, we skip joinable class logic
    if (alreadyJoined) {
      return new BookingStateResponse(
          true, joinedClassId, List.of() // no need to compute or return joinable classes
          );
    }

    // Not joined → compute joinable classes
    List<TutorClass> enrollableClasses =
        tutorClassRepository.findByCourse_IdAndIsDeletedFalse(courseId).stream()
            .filter(TutorClass::canEnroll)
            .toList();

    List<Long> classIds = enrollableClasses.stream().map(TutorClass::getId).toList();

    Map<Long, Integer> sessionCountMap = new HashMap<>();
    if (!classIds.isEmpty()) {
      List<Object[]> sessionCounts =
          classSessionRepository.countUpcomingSessionsByClassIds(classIds, LocalDate.now());

      for (Object[] row : sessionCounts) {
        Long classId = (Long) row[0];
        Long count = (Long) row[1];
        sessionCountMap.put(classId, count.intValue());
      }
    }

    List<BookingStateResponse.ClassSummary> openClasses =
        enrollableClasses.stream()
            .map(
                tc ->
                    BookingStateResponse.ClassSummary.fromEntity(
                        tc, sessionCountMap.getOrDefault(tc.getId(), 0)))
            .toList();

    return new BookingStateResponse(false, null, openClasses);
  }

  public void approveBooking(Long bookingId) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

    // If self-paced course, auto-assign to a class
    if (booking.getCourse().getType() == CourseType.SELF_PACED) {
      TutorClass classToJoin =
          tutorClassRepository.findByCourse_IdAndIsDeletedFalse(booking.getCourse().getId()).get(0);

      if (classToJoin != null) {
        for (BookingMember member : booking.getBookingMembers()) {
          userRepository
              .findById(member.getUserId())
              .ifPresent(
                  user -> {
                    ClassEnrollment enrollment = new ClassEnrollment();
                    enrollment.setStudent(user);
                    enrollment.setTutorClass(classToJoin);
                    classEnrollmentRepository.save(enrollment);
                  });
        }
      }
    } else {
      if (booking.getRegistrationType() == RegistrationType.TRIAL) {
        booking.setBookingStatus(BookingStatus.PAID);
      } else booking.setBookingStatus(BookingStatus.APPROVED);
    }
    bookingRepository.save(booking);

    // Send notifications to all approved members (excluding rejected)
    String courseName = booking.getCourse() != null ? booking.getCourse().getName() : "";
    String courseImage = booking.getCourse().getPictureUrl();
    User tutor = booking.getCourse().getTutor();
    String tutorName =
        tutor != null ? tutor.getFirstName() + " " + tutor.getLastName() : "the tutor";

    List<String> memberIds =
        booking.getBookingMembers().stream()
            .filter(member -> member.getStatus() != BookingMemberStatus.REJECTED)
            .map(BookingMember::getUserId)
            .collect(Collectors.toList());

    String studentMsg =
        "Your booking for course " + courseName + " with " + tutorName + " has been approved";
    notificationService.createAndSendNotifications(
        memberIds, studentMsg, NotificationType.TYPICAL, courseImage, null);
  }

  public void rejectBooking(Long bookingId) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

    booking.setBookingStatus(BookingStatus.REJECTED);
    bookingRepository.save(booking);

    // Send notifications to all members (excluding already rejected)
    String courseName = booking.getCourse() != null ? booking.getCourse().getName() : "";
    String courseImage = booking.getCourse() != null ? booking.getCourse().getPictureUrl() : null;
    User tutor = booking.getCourse() != null ? booking.getCourse().getTutor() : null;
    String tutorName =
        tutor != null ? tutor.getFirstName() + " " + tutor.getLastName() : "the tutor";

    List<String> memberIds =
        booking.getBookingMembers().stream()
            .filter(member -> member.getStatus() != BookingMemberStatus.REJECTED)
            .map(BookingMember::getUserId)
            .collect(Collectors.toList());

    String studentMsg =
        "Your booking for course " + courseName + " with " + tutorName + " has been rejected";
    notificationService.createAndSendNotifications(
        memberIds, studentMsg, NotificationType.TYPICAL, courseImage, null);
  }

  private String buildScheduleDescription(List<WeeklySchedule> weeklySchedules, Integer lessons) {
    if (weeklySchedules == null || weeklySchedules.isEmpty()) {
      return null;
    }

    String schedulePart =
        weeklySchedules.stream()
            .filter(Objects::nonNull)
            .map(this::formatScheduleEntry)
            .collect(Collectors.joining(" | "));

    if (lessons != null && lessons > 0) {
      return schedulePart + " | Total Lessons: " + lessons;
    }

    return schedulePart;
  }

  private String formatScheduleEntry(WeeklySchedule schedule) {
    String dayLabel = DAY_OF_WEEK_LABELS[schedule.getDayOfWeek()];
    String slotsLabel = formatSlots(schedule.getSlotNumbers());
    return dayLabel + ": " + slotsLabel;
  }

  private String formatSlots(List<Integer> slotNumbers) {
    return slotNumbers.stream()
        .filter(Objects::nonNull)
        .sorted()
        .map(this::formatSlot)
        .collect(Collectors.joining(", "));
  }

  private String formatSlot(Integer slotNumber) {
    return TeachingSlot.findBySlotNumber(slotNumber)
        .map(
            slot ->
                String.format(
                    "%s-%s",
                    slot.getStartTime().format(SLOT_TIME_FORMATTER),
                    slot.getEndTime().format(SLOT_TIME_FORMATTER)))
        .orElseGet(() -> "Slot " + slotNumber);
  }

  private Set<String> resolveMemberIds(
      CreateBookingRequest request, User currentUser, boolean isSelfPaced) {
    Set<String> memberIds = new HashSet<>();

    if (!isSelfPaced && request.getGroupType() == GroupType.GROUP) {
      if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
        throw new AppException(ErrorCode.BOOKING_GROUP_MEMBERS_REQUIRED);
      }
    }

    if (request.getMemberIds() != null) {
      for (String memberId : request.getMemberIds()) {
        if (memberId == null) {
          continue;
        }
        userRepository
            .findByIdAndNotDeleted(memberId)
            .map(User::getUserId)
            .ifPresent(memberIds::add);
      }
    }

    memberIds.add(currentUser.getUserId());
    return memberIds;
  }

  private void attachBookingMembers(Booking booking, Set<String> memberIds, String ownerId) {
    // Đảm bảo tập bookingMembers không null để tránh NPE khi add
    if (booking.getBookingMembers() == null) {
      booking.setBookingMembers(new HashSet<>());
    }

    for (String memberId : memberIds) {
      String role =
          memberId.equals(ownerId)
              ? BookingMemberRoles.OWNER.name()
              : BookingMemberRoles.MEMBER.name();
      BookingMember bookingMember =
          BookingMember.builder()
              .booking(booking)
              .userId(memberId)
              .role(role)
              .status(
                  memberId.equals(ownerId)
                      ? BookingMemberStatus.APPROVED
                      : BookingMemberStatus.WAITING)
              .build();
      booking.getBookingMembers().add(bookingMember);
    }
  }

  private BookingResponse toBookingResponse(Booking booking) {
    return toBookingResponse(booking, Collections.emptyMap());
  }

  private BookingResponse toBookingResponse(Booking booking, Map<String, User> userMap) {
    Course course = booking.getCourse();
    BookingResponse.CourseInfo courseInfo = null;
    if (course != null) {
      courseInfo =
          BookingResponse.CourseInfo.builder()
              .id(course.getId())
              .name(course.getName())
              .type(course.getType())
              .status(course.getStatus())
              .price(course.getPrice())
              .combo(course.getIsCombo())
              .build();
    }

    List<BookingResponse.MemberInfo> members =
        booking.getBookingMembers() == null
            ? Collections.emptyList()
            : booking.getBookingMembers().stream()
                .map(
                    member -> {
                      String memberName = "Unknown User";

                      // Try to get user from provided map first
                      User user = userMap.get(member.getUserId());

                      // If not in map, fetch individually (for backward compatibility)
                      if (user == null) {
                        user = userRepository.findById(member.getUserId()).orElse(null);
                      }

                      if (user != null) {
                        memberName = user.getFirstName() + " " + user.getLastName();
                      }

                      return BookingResponse.MemberInfo.builder()
                          .userId(member.getUserId())
                          .role(member.getRole())
                          .name(memberName)
                          .status(member.getStatus())
                          .build();
                    })
                .collect(Collectors.toList());

    return BookingResponse.builder()
        .id(booking.getId())
        .bookingStatus(booking.getBookingStatus())
        .registrationType(booking.getRegistrationType())
        .groupType(booking.getGroupType())
        .totalAmount(booking.getTotalAmount())
        .scheduleDescription(booking.getScheduleDescription())
        .course(courseInfo)
        .members(members)
        .build();
  }

  @Transactional
  public void updateOrRejectInvite(Long bookingId, boolean accept, Long notificationId) {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    BookingMember bookingMember =
        bookingMemberRepository
            .findByUserIdAndBookingId(currentUser.getUserId(), bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

    // Reject -> Minus total amount from booking
    if (!accept) {
      Booking booking = bookingMember.getBooking();
      long activeMembers =
          booking.getBookingMembers().stream()
              .filter(m -> m.getStatus() != BookingMemberStatus.REJECTED)
              .count();
      long remainingMembers = activeMembers - 1;

      BigDecimal amountPerMember =
          booking
              .getTotalAmount()
              .divide(BigDecimal.valueOf(activeMembers), 2, RoundingMode.HALF_UP);

      BigDecimal newTotalAmount = amountPerMember.multiply(BigDecimal.valueOf(remainingMembers));

      booking.setTotalAmount(newTotalAmount);
    }
    bookingMember.setStatus(accept ? BookingMemberStatus.APPROVED : BookingMemberStatus.REJECTED);
    bookingMemberRepository.save(bookingMember);

    // Update notification: change from BOOKING_INVITE to TYPICAL with no action link
    NotificationUpdateRequest updateRequest =
        NotificationUpdateRequest.builder().type(NotificationType.TYPICAL).actionLink(null).build();

    String message =
        accept
            ? "You have accepted the booking invitation"
            : "You have rejected the booking invitation";
    updateRequest.setMessage(message);

    notificationService.updateNotification(notificationId, currentUser.getUserId(), updateRequest);
  }

  // Admin methods
  public Page<BookingAdminListItemResponse> getAdminBookingList(
      BookingStatus status, String search, int page, int size, String sortBy) {

    // Mặc định sort theo createdAt DESC nếu không có sortBy
    String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
    org.springframework.data.domain.Sort sort =
        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField);
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Booking> bookings;

    if (search != null && !search.trim().isEmpty()) {
      String searchTerm = search.trim();
      bookings = bookingRepository.searchByStatusAndTerm(status, searchTerm, pageable);
    } else {
      if (status != null) {
        bookings = bookingRepository.findByBookingStatus(status, pageable);
      } else {
        bookings = bookingRepository.findAll(pageable);
      }
    }

    return bookings.map(this::toBookingAdminListItem);
  }

  public BookingAdminDetailResponse getAdminBookingDetail(Long bookingId) {
    Booking booking =
        bookingRepository
            .findWithDetailsById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

    return toBookingAdminDetail(booking);
  }

  private BookingAdminListItemResponse toBookingAdminListItem(Booking booking) {
    Course course = booking.getCourse();
    Set<BookingMember> members = booking.getBookingMembers();

    int pendingCount =
        (int) members.stream().filter(m -> m.getStatus() == BookingMemberStatus.WAITING).count();

    boolean hasUnpaid =
        booking.getTransactions() != null
            && booking.getTransactions().stream()
                .anyMatch(t -> t.getStatus() != PaymentStatus.PAID);

    return BookingAdminListItemResponse.builder()
        .id(booking.getId())
        .bookingStatus(booking.getBookingStatus())
        .registrationType(booking.getRegistrationType())
        .groupType(booking.getGroupType())
        .totalAmount(booking.getTotalAmount())
        .scheduleDescription(booking.getScheduleDescription())
        .courseName(course != null ? course.getName() : null)
        .memberCount(members.size())
        .pendingMemberCount(pendingCount)
        .hasUnpaidTransactions(hasUnpaid)
        .createdAt(booking.getCreatedAt())
        .createdBy(booking.getCreatedBy())
        .build();
  }

  private BookingAdminDetailResponse toBookingAdminDetail(Booking booking) {
    Set<BookingMember> members = booking.getBookingMembers();

    // Find owner
    BookingMember owner =
        members.stream()
            .filter(m -> BookingMemberRoles.OWNER.name().equals(m.getRole()))
            .findFirst()
            .orElse(null);

    // Get rejected members
    List<BookingResponse.MemberInfo> rejectedMembers =
        members.stream()
            .filter(m -> m.getStatus() == BookingMemberStatus.REJECTED)
            .map(
                m ->
                    BookingResponse.MemberInfo.builder()
                        .userId(m.getUserId())
                        .role(m.getRole())
                        .status(m.getStatus())
                        .build())
            .collect(Collectors.toList());

    // Count by status
    int approvedCount =
        (int) members.stream().filter(m -> m.getStatus() == BookingMemberStatus.APPROVED).count();
    int waitingCount =
        (int) members.stream().filter(m -> m.getStatus() == BookingMemberStatus.WAITING).count();
    int rejectedCount = rejectedMembers.size();

    // Check payment status and build transaction summaries
    Set<Transaction> transactions = booking.getTransactions();
    List<BookingAdminDetailResponse.TransactionSummary> transactionSummaries =
        Collections.emptyList();
    boolean isPaid = false;

    if (transactions != null && !transactions.isEmpty()) {
      // isPaid is true if any transaction has status PAID
      isPaid = transactions.stream().anyMatch(t -> t.getStatus() == PaymentStatus.PAID);

      transactionSummaries =
          transactions.stream()
              .map(
                  t ->
                      BookingAdminDetailResponse.TransactionSummary.builder()
                          .reference(t.getTransactionId())
                          .gateway(PAYMENT_GATEWAY_PAYOS)
                          .status(t.getStatus().name())
                          .amount(t.getAmount())
                          .occurredAt(t.getPaymentDate().atZone(ZoneId.systemDefault()).toInstant())
                          .build())
              .collect(Collectors.toList());
    }

    return BookingAdminDetailResponse.builder()
        .booking(toBookingResponse(booking))
        .bookingOwnerUserId(owner != null ? owner.getUserId() : null)
        .bookingOwner(
            owner != null
                ? BookingResponse.MemberInfo.builder()
                    .userId(owner.getUserId())
                    .role(owner.getRole())
                    .status(owner.getStatus())
                    .build()
                : null)
        .memberCount(members.size())
        .waitingMemberCount(waitingCount)
        .approvedMemberCount(approvedCount)
        .rejectedMemberCount(rejectedCount)
        .rejectedMembers(rejectedMembers)
        .totalAmount(booking.getTotalAmount())
        .isPaid(isPaid)
        .transactions(transactionSummaries)
        .createdAt(booking.getCreatedAt())
        .updatedAt(booking.getModifiedAt())
        .createdBy(booking.getCreatedBy())
        .lastModifiedBy(booking.getModifiedBy())
        .build();
  }

  public List<AvailableTrialSessionsResponse> getAvailableTrialSessions(Long classId) {
    TutorClass tutorClass =
        tutorClassRepository
            .findByIdAndIsDeletedFalse(Math.toIntExact(classId))
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

    List<ClassSession> upcomingSessions =
        classSessionRepository.findUpcomingSessionsByClassId(
            classId, LocalDate.now(), PageRequest.of(0, 2));

    return upcomingSessions.stream()
        .map(
            session -> {
              String timeRange =
                  TeachingSlot.findBySlotNumber(session.getSlotNumber())
                      .map(
                          slot ->
                              String.format(
                                  "%s-%s",
                                  slot.getStartTime().format(SLOT_TIME_FORMATTER),
                                  slot.getEndTime().format(SLOT_TIME_FORMATTER)))
                      .orElse("TBD");

              return AvailableTrialSessionsResponse.builder()
                  .sessionId(session.getId())
                  .sessionNumber(session.getSessionNumber())
                  .topic(session.getTopic())
                  .sessionDate(session.getSessionDate())
                  .dayOfWeek(session.getSessionDate().getDayOfWeek().toString())
                  .slotNumber(session.getSlotNumber())
                  .timeRange(timeRange)
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public BookingResponse bookTrialLesson(BookTrialRequest request) {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    // Check if user already had a trial for this course
    boolean hasTrialForCourse =
        bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
            currentUser.getUserId(), request.getCourseId(), RegistrationType.TRIAL);

    if (hasTrialForCourse) {
      throw new AppException(ErrorCode.TRIAL_ALREADY_USED);
    }

    // Validate class
    TutorClass tutorClass =
        tutorClassRepository
            .findByIdAndIsDeletedFalse(Math.toIntExact(request.getClassId()))
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

    if (tutorClass.isFull()) {
      throw new AppException(ErrorCode.CLASS_FULL);
    }

    // Validate session
    ClassSession session =
        classSessionRepository
            .findByIdAndIsDeletedFalse(request.getSessionId())
            .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

    if (!session.getTutorClass().getId().equals(tutorClass.getId())) {
      throw new AppException(ErrorCode.SESSION_NOT_IN_CLASS);
    }

    if (session.getSessionDate().isBefore(LocalDate.now())) {
      throw new AppException(ErrorCode.SESSION_ALREADY_PASSED);
    }

    // Check for schedule overlap with student's existing classes
    String overlapCheck =
        checkScheduleOverlap(
            Collections.singletonList(currentUser.getUserId()),
            session.getSessionDate(),
            session.getSlotNumber(),
            null,
            null,
            -1L); // No class to exclude for trial bookings
    if (!"OK".equals(overlapCheck)) {
      throw new AppException(ErrorCode.OVERLAP_STUDENT_SCHEDULE);
    }

    // Build schedule description - just text with session details
    String scheduleDescription = buildTrialScheduleDescription(session, tutorClass);

    // Create booking
    Booking booking =
        Booking.builder()
            .course(tutorClass.getCourse())
            .groupType(GroupType.INDIVIDUAL)
            .registrationType(RegistrationType.TRIAL)
            .bookingStatus(BookingStatus.PENDING)
            .totalAmount(BigDecimal.ZERO)
            .scheduleDescription(scheduleDescription)
            .build();

    attachBookingMembers(
        booking, Collections.singleton(currentUser.getUserId()), currentUser.getUserId());
    Booking savedBooking = bookingRepository.save(booking);

    // Build user map (only current user in this case)
    Map<String, User> userMap = new HashMap<>();
    userMap.put(currentUser.getUserId(), currentUser);

    // Notifications
    String courseName = tutorClass.getCourse() != null ? tutorClass.getCourse().getName() : "";

    notificationService.createAndSendNotification(
        currentUser.getUserId(),
        "Your trial lesson request for " + courseName + " has been submitted, waiting for approval",
        NotificationType.TYPICAL,
        null,
        null);

    if (tutorClass.getTutor() != null) {
      notificationService.createAndSendNotification(
          tutorClass.getTutor().getUserId(),
          currentUser.getFirstName()
              + " "
              + currentUser.getLastName()
              + " requested a trial lesson for class "
              + tutorClass.getTitle(),
          NotificationType.TYPICAL,
          null,
          NotificationLink.TUTOR_BOOKING.getPrefix());
    }

    return toBookingResponse(savedBooking, userMap);
  }

  private String buildTrialScheduleDescription(ClassSession session, TutorClass tutorClass) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");
    String formattedDate = session.getSessionDate().format(dateFormatter);

    String slotTime =
        TeachingSlot.findBySlotNumber(session.getSlotNumber())
            .map(
                slot ->
                    String.format(
                        "%s-%s",
                        slot.getStartTime().format(SLOT_TIME_FORMATTER),
                        slot.getEndTime().format(SLOT_TIME_FORMATTER)))
            .orElse("Slot " + session.getSlotNumber());

    return String.format(
        "Trial Lesson | Class:%s - %s | Session #%d: %s | %s | %s | SessionId:%d",
        tutorClass.getId(),
        tutorClass.getTitle(),
        session.getSessionNumber(),
        session.getTopic() != null ? session.getTopic() : "General lesson",
        formattedDate,
        slotTime,
        session.getId());
  }

  public List<BookingResponse> getMyMissingBookings() {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser =
        userRepository
            .findByUsernameAndNotDeleted(currentUsername)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    // Find all PAID bookings for courses taught by this tutor
    List<Booking> paidBookings =
        bookingRepository.findByBookingStatusAndCourseTutorUserId(
            BookingStatus.PAID, currentUser.getUserId());

    if (paidBookings.isEmpty()) {
      return List.of();
    }

    // Collect all unique course IDs
    Set<Long> courseIds =
        paidBookings.stream()
            .filter(booking -> booking.getCourse() != null)
            .map(booking -> booking.getCourse().getId())
            .collect(Collectors.toSet());

    if (courseIds.isEmpty()) {
      return List.of();
    }

    // Batch fetch all enrolled student IDs for all courses in a single query
    List<Object[]> enrollmentResults =
        classEnrollmentRepository.findDistinctStudentUserIdsByCourseIds(new ArrayList<>(courseIds));

    // Build a map: courseId -> Set of enrolled student user IDs
    Map<Long, Set<String>> enrolledStudentsByCourseId = new HashMap<>();
    for (Object[] result : enrollmentResults) {
      Long courseId = (Long) result[0];
      String studentUserId = (String) result[1];
      enrolledStudentsByCourseId.computeIfAbsent(courseId, k -> new HashSet<>()).add(studentUserId);
    }

    // Filter bookings where at least one member is not enrolled in any class for that course
    List<Booking> missingBookings =
        paidBookings.stream()
            .filter(
                booking -> {
                  if (booking.getCourse() == null || booking.getBookingMembers() == null) {
                    return false;
                  }

                  Long courseId = booking.getCourse().getId();
                  // Get enrolled student IDs from the pre-loaded map (empty set if no enrollments)
                  Set<String> enrolledStudentIds =
                      enrolledStudentsByCourseId.getOrDefault(courseId, Collections.emptySet());

                  // Check if any booking member is NOT enrolled
                  return booking.getBookingMembers().stream()
                      .anyMatch(member -> !enrolledStudentIds.contains(member.getUserId()));
                })
            .toList();

    // Collect all unique user IDs from booking members
    Set<String> userIds =
        missingBookings.stream()
            .filter(booking -> booking.getBookingMembers() != null)
            .flatMap(booking -> booking.getBookingMembers().stream())
            .map(BookingMember::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Batch fetch all users in a single query
    Map<String, User> userMap = new HashMap<>();
    if (!userIds.isEmpty()) {
      List<User> users = userRepository.findAllById(userIds);
      userMap = users.stream().collect(Collectors.toMap(User::getUserId, user -> user));
    }

    // Convert to response DTOs using the pre-loaded user map
    final Map<String, User> finalUserMap = userMap;
    return missingBookings.stream()
        .map(booking -> toBookingResponse(booking, finalUserMap))
        .collect(Collectors.toList());
  }

  private Long parseSessionIdFromDescription(String scheduleDescription) {
    if (scheduleDescription == null || scheduleDescription.isEmpty()) {
      return null;
    }

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("SessionId:(\\d+)");
    java.util.regex.Matcher matcher = pattern.matcher(scheduleDescription);

    if (matcher.find()) {
      try {
        return Long.parseLong(matcher.group(1));
      } catch (NumberFormatException e) {
        log.warn("Failed to parse sessionId from description: {}", scheduleDescription);
        return null;
      }
    }

    return null;
  }

  @Transactional
  @Scheduled(fixedDelay = 5 * 60 * 1000)
  public void cleanupTrialBookingsAfterSession() {
    log.info("Starting cleanup of trial bookings after session completion");
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    List<Booking> trialBookings =
        bookingRepository.findByRegistrationTypeAndBookingStatus(
            RegistrationType.TRIAL, BookingStatus.APPROVED);

    log.info("Found {} trial bookings to process", trialBookings.size());

    for (Booking booking : trialBookings) {
      Long sessionId = parseSessionIdFromDescription(booking.getScheduleDescription());
      if (sessionId == null) {
        log.info("Booking {} has no sessionId in description, skipping", booking.getId());
        continue;
      }

      ClassSession session =
          classSessionRepository.findByIdAndIsDeletedFalse(sessionId).orElse(null);

      if (session == null) {
        log.info("Session {} not found for booking {}", sessionId, booking.getId());
        continue;
      }

      boolean sessionPassed = false;
      if (session.getSessionDate().isBefore(today)) {
        sessionPassed = true;
      } else if (session.getSessionDate().equals(today)) {
        TeachingSlot slot = TeachingSlot.findBySlotNumber(session.getSlotNumber()).orElse(null);
        if (slot != null && now.isAfter(slot.getEndTime())) {
          sessionPassed = true;
        }
      }

      if (!sessionPassed) {
        continue;
      }

      TutorClass tutorClass = session.getTutorClass();
      if (tutorClass == null) {
        log.info("TutorClass not found for session {}", sessionId);
        continue;
      }

      String userId =
          booking.getBookingMembers().stream()
              .findFirst()
              .map(BookingMember::getUserId)
              .orElse(null);

      if (userId == null) {
        log.info("No booking member found for booking {}", booking.getId());
        continue;
      }

      Optional<ClassEnrollment> enrollmentOpt =
          classEnrollmentRepository.findByTutorClassIdAndStudentUserId(tutorClass.getId(), userId);

      if (enrollmentOpt.isPresent()) {
        ClassEnrollment enrollment = enrollmentOpt.get();

        String courseName = booking.getCourse() != null ? booking.getCourse().getName() : "course";
        notificationService.createAndSendNotification(
            userId,
            "Bạn đã hoàn thành buổi học thử cho khóa học " + courseName,
            NotificationType.TYPICAL,
            null,
            null);

        classEnrollmentRepository.delete(enrollment);
        log.info(
            "Deleted ClassEnrollment for trial booking {} - userId: {}, classId: {}",
            booking.getId(),
            userId,
            tutorClass.getId());
      } else {
        log.info(
            "No ClassEnrollment found for userId: {}, classId: {}", userId, tutorClass.getId());
      }
    }
  }

  /**
   * Unified method to check schedule overlaps for booking members/owner.
   * Handles 3 scenarios:
   * 1. Single session check (sessionDate + slotNumber provided)
   * 2. Class sessions check (classId provided)
   * 3. Weekly schedules check (weeklySchedules provided)
   *
   * @param memberIds Collection of user IDs to check (Set or List)
   * @param sessionDate Optional: specific session date for single session check
   * @param slotNumber Optional: specific slot number for single session check
   * @param classId Optional: class ID to check all future sessions
   * @param weeklySchedules Optional: weekly schedules for recurring booking check
   * @param excludeClassId Class ID to exclude from conflict check (-1 to not exclude any)
   * @return "OK" if no overlap, "OVERLAP" otherwise
   */
  private String checkScheduleOverlap(
      Collection<String> memberIds,
      LocalDate sessionDate,
      Integer slotNumber,
      Long classId,
      List<WeeklySchedule> weeklySchedules,
      Long excludeClassId) {

    if (memberIds == null || memberIds.isEmpty()) {
      return "OK";
    }

    List<String> memberIdsList =
        memberIds instanceof List ? (List<String>) memberIds : new ArrayList<>(memberIds);

    // Case 1: Single session overlap check
    if (sessionDate != null && slotNumber != null) {
      long conflictCount =
          classSessionRepository.countStudentConflicts(
              memberIdsList, sessionDate, slotNumber, excludeClassId != null ? excludeClassId : -1L);

      if (conflictCount > 0) {
        log.warn(
            "Student(s) have {} overlapping session(s) on {} at slot {}",
            conflictCount,
            sessionDate,
            slotNumber);
        return "OVERLAP";
      }
      return "OK";
    }

    // Case 2: Class sessions overlap check
    if (classId != null) {
      List<ClassSession> futureSessions =
          classSessionRepository.findUpcomingSessionsByClassId(classId, LocalDate.now(), null);

      if (futureSessions != null && !futureSessions.isEmpty()) {
        for (ClassSession session : futureSessions) {
          String result =
              checkScheduleOverlap(
                  memberIdsList,
                  session.getSessionDate(),
                  session.getSlotNumber(),
                  null,
                  null,
                  classId);

          if (!"OK".equals(result)) {
            log.warn(
                "Student(s) have overlapping session with class {} session on {} at slot {}",
                classId,
                session.getSessionDate(),
                session.getSlotNumber());
            return "OVERLAP";
          }
        }
      }
      return "OK";
    }

    // Case 3: Weekly schedules overlap check
    if (weeklySchedules != null && !weeklySchedules.isEmpty()) {
      for (WeeklySchedule schedule : weeklySchedules) {
        if (schedule == null
            || schedule.getDayOfWeek() == null
            || schedule.getSlotNumbers() == null) {
          continue;
        }

        for (Integer slot : schedule.getSlotNumbers()) {
          if (slot == null) {
            continue;
          }

          // Check 4 weeks ahead
          LocalDate startDate = LocalDate.now();
          LocalDate endDate = startDate.plusWeeks(4);

          // Find first occurrence of target day
          LocalDate currentDate = startDate;
          int targetDayOfWeek = schedule.getDayOfWeek();
          int currentDayOfWeek = currentDate.getDayOfWeek().getValue() % 7;
          int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
          currentDate = currentDate.plusDays(daysToAdd);

          // Check each occurrence
          while (!currentDate.isAfter(endDate)) {
            long conflictCount =
                classSessionRepository.countStudentConflicts(
                    memberIdsList, currentDate, slot, -1L);

            if (conflictCount > 0) {
              log.warn(
                  "Booking members have {} overlapping session(s) on {} at slot {}",
                  conflictCount,
                  currentDate,
                  slot);
              return "OVERLAP";
            }

            currentDate = currentDate.plusWeeks(1);
          }
        }
      }
      return "OK";
    }

    return "OK";
  }
}
