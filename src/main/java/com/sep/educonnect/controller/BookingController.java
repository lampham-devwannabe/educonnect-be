package com.sep.educonnect.controller;

import com.sep.educonnect.dto.booking.*;
import com.sep.educonnect.dto.booking.admin.BookingAdminDetailResponse;
import com.sep.educonnect.dto.booking.admin.BookingAdminListItemResponse;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.service.BookingService;
import com.sep.educonnect.service.I18nService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/booking")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

  BookingService bookingService;
  I18nService i18nService;

  @PostMapping
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<BookingResponse> createBooking(
      @RequestBody @Valid CreateBookingRequest request) {
    BookingResponse booking = bookingService.createBooking(request);
    return ApiResponse.<BookingResponse>builder().result(booking).build();
  }

  @PostMapping("/join")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<BookingResponse> joinClass(@RequestBody @Valid JoinClassRequest request) {
    BookingResponse booking = bookingService.joinClass(request);
    return ApiResponse.<BookingResponse>builder().result(booking).build();
  }

  @PostMapping("/trial")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<BookingResponse> bookTrialLesson(
      @RequestBody @Valid BookTrialRequest request) {
    BookingResponse booking = bookingService.bookTrialLesson(request);
    return ApiResponse.<BookingResponse>builder().result(booking).build();
  }

  @GetMapping("/trial/sessions/{classId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<List<AvailableTrialSessionsResponse>> getAvailableTrialSessions(
      @PathVariable Long classId) {
    return ApiResponse.<List<AvailableTrialSessionsResponse>>builder()
        .result(bookingService.getAvailableTrialSessions(classId))
        .build();
  }

  @PostMapping("/{bookingId}/approve")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public ApiResponse<Void> approveBooking(@PathVariable Long bookingId) {
    bookingService.approveBooking(bookingId);
    return ApiResponse.<Void>builder().message(i18nService.msg("msg.booking.success")).build();
  }

  @PostMapping("/{bookingId}/reject")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public ApiResponse<Void> rejectBooking(@PathVariable Long bookingId) {
    bookingService.rejectBooking(bookingId);
    return ApiResponse.<Void>builder().message(i18nService.msg("msg.booking.reject")).build();
  }

  @GetMapping("/{courseId}")
  public ApiResponse<BookingStateResponse> checkUserBookingForCourse(@PathVariable Long courseId) {
    return ApiResponse.<BookingStateResponse>builder()
        .result(bookingService.getBookingState(courseId))
        .build();
  }

  @GetMapping("/admin/list")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public ApiResponse<Page<BookingAdminListItemResponse>> getAdminBookingList(
      @RequestParam(required = false) BookingStatus status,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sortBy) {

    Page<BookingAdminListItemResponse> bookings =
        bookingService.getAdminBookingList(status, search, page, size, sortBy);

    return ApiResponse.<Page<BookingAdminListItemResponse>>builder().result(bookings).build();
  }

  @GetMapping("/admin/{bookingId}")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public ApiResponse<BookingAdminDetailResponse> getAdminBookingDetail(
      @PathVariable @Min(1) Long bookingId) {

    return ApiResponse.<BookingAdminDetailResponse>builder()
        .result(bookingService.getAdminBookingDetail(bookingId))
        .build();
  }
}
