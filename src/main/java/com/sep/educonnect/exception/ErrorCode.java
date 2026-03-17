package com.sep.educonnect.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION(9999, "error.uncategorized", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_KEY(1001, "error.invalid.key", HttpStatus.BAD_REQUEST),
  USER_EXISTED(1002, "error.user.existed", HttpStatus.BAD_REQUEST),
  USERNAME_INVALID(1003, "error.username.invalid", HttpStatus.BAD_REQUEST),
  INVALID_PASSWORD(1004, "error.password.invalid", HttpStatus.BAD_REQUEST),
  USER_NOT_EXISTED(1005, "error.user.not.existed", HttpStatus.NOT_FOUND),
  UNAUTHENTICATED(1006, "error.unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1007, "error.unauthorized", HttpStatus.FORBIDDEN),
  INVALID_DOB(1008, "error.dob.invalid", HttpStatus.BAD_REQUEST),
  ROLE_NOT_EXISTED(1009, "error.role.not.existed", HttpStatus.BAD_REQUEST),
  INVALID_TOKEN(1010, "error.token.invalid", HttpStatus.BAD_REQUEST),
  TOKEN_EXPIRED(1011, "error.token.expired", HttpStatus.BAD_REQUEST),
  SUBJECT_EXISTED(1012, "error.subject.existed", HttpStatus.BAD_REQUEST),
  SUBJECT_NOT_EXISTED(1013, "error.subject.not.existed", HttpStatus.NOT_FOUND),
  SYLLABUS_EXISTED(1014, "error.syllabus.existed", HttpStatus.BAD_REQUEST),
  SYLLABUS_NOT_EXISTED(1015, "error.syllabus.not.existed", HttpStatus.NOT_FOUND),
  TUTOR_PROFILE_NOT_EXISTED(1016, "error.tutor.not.existed", HttpStatus.BAD_REQUEST),
  INVALID_DOCUMENT_TYPE(1017, "error.file.type.invalid", HttpStatus.BAD_REQUEST),
  TUTOR_AVAILABILITY_NOT_SET(1018, "error.tutor.availability.not.set", HttpStatus.NOT_FOUND),
  CURRENT_PASSWORD_INCORRECT(1019, "error.current.password.incorrect", HttpStatus.BAD_REQUEST),
  PASSWORD_MISMATCH(1020, "error.password.mismatch", HttpStatus.BAD_REQUEST),
  CURRENT_PASSWORD_REQUIRED(1021, "error.current.password.required", HttpStatus.BAD_REQUEST),
  NEW_PASSWORD_REQUIRED(1022, "error.new.password.required", HttpStatus.BAD_REQUEST),
  CONFIRM_PASSWORD_REQUIRED(1023, "error.confirm.password.required", HttpStatus.BAD_REQUEST),
  TUTOR_NOT_AVAILABLE(1024, "error.tutor.not.available", HttpStatus.BAD_REQUEST),
  COURSE_NOT_EXISTED(1025, "error.course.not.set", HttpStatus.NOT_FOUND),
  CLASS_NOT_FOUND(1026, "error.class.not.found", HttpStatus.NOT_FOUND),
  SESSION_NOT_EXISTED(1027, "error.session.not.existed", HttpStatus.NOT_FOUND),
  GET_MEETING_FAILED(1028, "error.meeting.failed", HttpStatus.BAD_REQUEST),
  NO_CLASSES_FOUND(1029, "error.class.not.found", HttpStatus.NOT_FOUND),
  PASSWORD_NOT_MATCHED(1030, "error.password.not.matched", HttpStatus.BAD_REQUEST),

  // Exception handling codes (3000 block)
  EXCEPTION_NOT_FOUND(3001, "error.exception.not.found", HttpStatus.NOT_FOUND),
  EXCEPTION_ALREADY_EXISTS(3002, "error.exception.already.exists", HttpStatus.CONFLICT),
  EXCEPTION_TOO_LATE(3003, "error.exception.too.late", HttpStatus.BAD_REQUEST),
  EXCEPTION_LIMIT_EXCEEDED(3004, "error.exception.limit.exceeded", HttpStatus.BAD_REQUEST),
  NO_VALID_SESSIONS(3005, "error.exception.no.valid.sessions", HttpStatus.BAD_REQUEST),
  CANNOT_MODIFY_PROCESSED_EXCEPTION(
      3006, "error.exception.cannot.modify.processed", HttpStatus.BAD_REQUEST),
  CANNOT_CANCEL_PROCESSED_EXCEPTION(
      3007, "error.exception.cannot.cancel.processed", HttpStatus.BAD_REQUEST),
  EXCEPTION_ALREADY_PROCESSED(3008, "error.exception.already.processed", HttpStatus.BAD_REQUEST),
  REJECTION_REASON_REQUIRED(
      3009, "error.exception.rejection.reason.required", HttpStatus.BAD_REQUEST),
  CANNOT_MODIFY_PAST_SESSION(
      3010, "error.exception.cannot.modify.past.session", HttpStatus.BAD_REQUEST),
  INVALID_SUBMISSION_STATUS(3011, "error.invalid.submission.status", HttpStatus.BAD_REQUEST),
  MISSING_REQUIRED_DOCUMENTS(3012, "error.missing.required.documents", HttpStatus.BAD_REQUEST),
  INCOMPLETE_PROFILE(3013, "error.incomplete.profile", HttpStatus.BAD_REQUEST),
  DOCUMENT_NOT_FOUND(3014, "error.document.not.found", HttpStatus.NOT_FOUND),
  INVALID_RESUBMISSION(3014, "error.invalid.resubmission", HttpStatus.BAD_REQUEST),
  PROCESS_NOT_FOUND(3015, "error.process.not.found", HttpStatus.NOT_FOUND),
  DOCUMENTS_NOT_APPROVED(3016, "error.documents.not.approved", HttpStatus.BAD_REQUEST),
  REVISION_NOTES_REQUIRED(3017, "error.revision.notes.required", HttpStatus.BAD_REQUEST),
  COMMENT_TEXT_REQUIRED(3018, "error.comment.text.required", HttpStatus.BAD_REQUEST),

  // Booking codes
  BOOKING_NOT_FOUND(3019, "error.booking.not.found", HttpStatus.NOT_FOUND),
  BOOKING_ALREADY_PAID(3020, "error.booking.already.paid", HttpStatus.BAD_REQUEST),
  BOOKING_MUST_BE_APPROVED(3021, "error.booking.must.be.approved", HttpStatus.BAD_REQUEST),
  CREATE_PAYMENT_QR_FAILED(3022, "error.create.payment.qr", HttpStatus.BAD_REQUEST),

  // Booking validations
  BOOKING_REGISTRATION_TYPE_REQUIRED(
      3023, "error.booking.registrationType.required", HttpStatus.BAD_REQUEST),
  BOOKING_REGISTRATION_TYPE_UNSUPPORTED(
      3024, "error.booking.registrationType.unsupported", HttpStatus.BAD_REQUEST),
  BOOKING_GROUP_TYPE_REQUIRED(3025, "error.booking.groupType.required", HttpStatus.BAD_REQUEST),
  BOOKING_PRICE_PER_LESSON_REQUIRED(
      3026, "error.booking.pricePerLesson.required", HttpStatus.BAD_REQUEST),
  BOOKING_PRICE_PER_LESSON_MIN(3027, "error.booking.pricePerLesson.min", HttpStatus.BAD_REQUEST),
  BOOKING_LESSONS_MIN(3028, "error.booking.lessons.min", HttpStatus.BAD_REQUEST),
  COURSE_INACTIVE(3029, "course.inactive", HttpStatus.BAD_REQUEST),
  BOOKING_GROUP_MEMBERS_REQUIRED(3030, "error.booking.members.required", HttpStatus.BAD_REQUEST),
  BOOKING_ALREADY_EXISTS(3031, "error.booking.exist", HttpStatus.BAD_REQUEST),
  CLASS_JOIN_DEADLINE_PASSED(3032, "error.class.deadline.passed", HttpStatus.BAD_REQUEST),
  TRIAL_ALREADY_USED(3033, "error.trial.already.used", HttpStatus.BAD_REQUEST),
  CLASS_FULL(3063, "error.class.full", HttpStatus.BAD_REQUEST),
  SESSION_NOT_FOUND(3064, "error.session.not.found", HttpStatus.NOT_FOUND),
  SESSION_NOT_IN_CLASS(3065, "error.session.not.in.class", HttpStatus.BAD_REQUEST),
  SESSION_ALREADY_PASSED(3066, "error.session.already.passed", HttpStatus.BAD_REQUEST),

  // Schedule change codes
  NEW_DATE_MUST_BE_FUTURE(3034, "error.schedule.newDate.future.required", HttpStatus.BAD_REQUEST),
  SCHEDULE_CHANGE_TOO_LATE(3035, "error.schedule.change.too.late", HttpStatus.BAD_REQUEST),
  OLD_AND_NEW_DATE_SAME(3036, "error.schedule.date.same", HttpStatus.BAD_REQUEST),
  SCHEDULE_CHANGE_ALREADY_EXISTS(
      3037, "error.schedule.change.already.exists", HttpStatus.BAD_REQUEST),
  SCHEDULE_CHANGE_NOT_FOUND(3038, "error.schedule.change.not.found", HttpStatus.NOT_FOUND),
  CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE(
      3039, "error.schedule.change.cannot.modify.processed", HttpStatus.BAD_REQUEST),
  CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE(
      3040, "error.schedule.change.cannot.cancel.processed", HttpStatus.BAD_REQUEST),
  INVALID_TEACHING_SLOT(3041, "error.invalid.teaching.slot", HttpStatus.BAD_REQUEST),
  CANNOT_SELF_DELETE(3042, "error.cannot.self.delete", HttpStatus.BAD_REQUEST),
  ENROLLMENT_NOT_EXISTED(3043, "error.enrollment.not.existed", HttpStatus.BAD_REQUEST),
  SUBJECT_IDS_REQUIRED(3044, "error.subject.ids.required", HttpStatus.BAD_REQUEST),
  SOME_SUBJECTS_NOT_FOUND(3045, "error.some.subjects.not.found", HttpStatus.NOT_FOUND),
  TAG_IDS_REQUIRED(3046, "error.tag.ids.required", HttpStatus.BAD_REQUEST),
  SOME_TAGS_NOT_FOUND(3047, "error.some.tags.not.found", HttpStatus.NOT_FOUND),
  STUDENT_ALREADY_ENROLLED(3048, "error.student.already.enrolled", HttpStatus.BAD_REQUEST),

  // Codes with original duplicates (now fixed)
  NOTIFICATION_NOT_EXISTED(3049, "error.notification.not.existed", HttpStatus.NOT_FOUND),
  LESSON_NOT_EXISTED(
      3050, "error.lesson.not.existed", HttpStatus.NOT_FOUND), // Changed from 3049 -> 3050
  MODULE_NOT_EXISTED(
      3051, "error.module.not.existed", HttpStatus.NOT_FOUND), // Changed from 3050 -> 3051
  EXAM_NOT_EXISTED(
      3052, "error.exam.not.existed", HttpStatus.NOT_FOUND), // Changed from 3051 -> 3052
  QUIZ_NOT_EXISTED(
      3053, "error.quiz.not.existed", HttpStatus.NOT_FOUND), // Changed from 3052 -> 3053
  SCHEDULE_CHANGE_ALREADY_PROCESSED(
      3054, "error.schedule.change.already.processed", HttpStatus.BAD_REQUEST), // Changed
  // from
  // 3053
  // ->
  // 3054
  TRANSACTION_NOT_FOUND(
      3055, "error.transaction.not.existed", HttpStatus.NOT_FOUND), // Changed from 3054 -> 3055
  STUDENT_NOT_ENROLLED_WITH_TUTOR(
      3056, "error.student.not.enrolled.with.tutor", HttpStatus.FORBIDDEN), // Changed
  // from 3054
  // -> 3056
  ALREADY_RATED(3057, "error.already.rated", HttpStatus.BAD_REQUEST),
  EXAM_SUBMISSION_NOT_FOUND(3058, "error.exam.submission.not.found", HttpStatus.NOT_FOUND),
  RATING_NOT_FOUND(
      3059, "error.rating.not.found", HttpStatus.NOT_FOUND), // Changed from 3056 -> 3059
  EXAM_NOT_ACCESSIBLE(
      3060, "error.exam.not.accessible", HttpStatus.FORBIDDEN), // Changed from 3056 -> 3060 (from
  // feat/api-exam)
  EXAM_NOT_PUBLISHED(
      3061, "error.exam.not.published", HttpStatus.BAD_REQUEST), // Changed from 3057 -> 3061 (from
  // feat/api-exam)
  INVALID_EXAM_OWNER(
      3062, "error.invalid.exam.owner", HttpStatus.FORBIDDEN), // Changed from 3058 -> 3062 (from
  // feat/api-exam)
  ALREADY_IN_WISHLIST(3063, "error.already.in.wishlist", HttpStatus.BAD_REQUEST),
  NOT_IN_WISHLIST(3064, "error.not.in.wishlist", HttpStatus.NOT_FOUND),
  // Email/Verification codes (4000 block)
  EMAIL_ALREADY_VERIFIED(4000, "error.email.already.verified", HttpStatus.BAD_REQUEST),
  EMAIL_NOT_VERIFIED(4001, "error.email.not.verified", HttpStatus.FORBIDDEN),
  VERIFICATION_TOKEN_INVALID(4002, "error.verification.token.invalid", HttpStatus.BAD_REQUEST),
  VERIFICATION_TOKEN_EXPIRED(4003, "error.verification.token.expired", HttpStatus.BAD_REQUEST),
  VERIFICATION_TOO_SOON(4004, "error.verification.too.soon", HttpStatus.TOO_MANY_REQUESTS),

  INVALID_PAYMENT_INFO(4005, "error.invalid.payment.info", HttpStatus.BAD_REQUEST),
  FAILED_TO_CHECK_STATUS(4006, "error.failed.to.check.status", HttpStatus.BAD_REQUEST),

  CANNOT_CANCEL_PAID_TRANSACTION(4007, "error.cannot.cancel.paid", HttpStatus.BAD_REQUEST),
  TAG_NOT_FOUND(4008, "error.tag.not.found", HttpStatus.NOT_FOUND),
  SESSION_MUST_REGISTER_EXCEPTION(4009, "session.must.register.exception", HttpStatus.BAD_REQUEST),

  OVERLAP_TUTOR_SCHEDULE(4010, "error.overlap.tutor.schedule", HttpStatus.BAD_REQUEST),
  OVERLAP_STUDENT_SCHEDULE(4011, "error.overlap.student.schedule", HttpStatus.BAD_REQUEST),

  INVALID_CLASS_SIZE(4012, "error.invalid.class.size", HttpStatus.BAD_REQUEST),
  INVALID_NUMBER_OF_SESSION(4013, "error.invalid.number.of.session", HttpStatus.BAD_REQUEST),
  INVALID_START_DATE(4014, "error.invalid.start.date", HttpStatus.BAD_REQUEST),
  INVALID_FILE(4015, "error.invalid.file", HttpStatus.BAD_REQUEST),

  // Video lesson codes (4100 block)
  VIDEO_LESSON_NOT_FOUND(4100, "error.video.lesson.not.found", HttpStatus.NOT_FOUND),
  VIDEO_NOT_READY_FOR_STREAMING(
      4101, "error.video.not.ready.for.streaming", HttpStatus.BAD_REQUEST),
  INVALID_RESUBMISSION_TIME(4102, "error.invalid.resubmission.time", HttpStatus.BAD_REQUEST),

  // Discussion codes (4200 block)
  DISCUSSION_NOT_FOUND(4200, "error.discussion.not.found", HttpStatus.NOT_FOUND),
  DISCUSSION_REPLY_NOT_FOUND(4201, "error.discussion.reply.not.found", HttpStatus.NOT_FOUND),

  // Progress codes (4300 block)
  COURSE_PROGRESS_ALREADY_EXISTS(4300, "error.course.progress.already.exists", HttpStatus.CONFLICT),
  COURSE_PROGRESS_NOT_FOUND(4301, "error.course.progress.not.found", HttpStatus.NOT_FOUND),
  COURSE_PROGRESS_STATE_INVALID(
      4302, "error.course.progress.state.invalid", HttpStatus.INTERNAL_SERVER_ERROR),
  CLASS_ENROLLMENT_NOT_FOUND(4303, "error.class.enrollment.not.found", HttpStatus.NOT_FOUND),
  TUTOR_CLASS_NOT_ASSOCIATED(4304, "error.tutor.class.not.associated", HttpStatus.BAD_REQUEST),
  COURSE_NOT_ASSOCIATED(4305, "error.course.not.associated", HttpStatus.BAD_REQUEST),
  SYLLABUS_NOT_DEFINED(4306, "error.syllabus.not.defined", HttpStatus.BAD_REQUEST),
  LESSON_PROGRESS_NOT_FOUND(4307, "error.lesson.progress.not.found", HttpStatus.NOT_FOUND);

  private final int code;
  private final String messageKey;
  private final HttpStatusCode statusCode;

  ErrorCode(int code, String messageKey, HttpStatusCode statusCode) {
    this.code = code;
    this.messageKey = messageKey;
    this.statusCode = statusCode;
  }

  // Keep backward compatibility for existing getMessage() calls
  @Deprecated
  public String getMessage() {
    return messageKey;
  }
}
