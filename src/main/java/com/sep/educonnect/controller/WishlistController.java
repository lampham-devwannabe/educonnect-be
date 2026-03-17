package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.student.WishlistResponse;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.WishlistService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WishlistController {
    WishlistService wishlistService;
    I18nService i18nService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<WishlistResponse>> getWishlist() {
        return ApiResponse.<List<WishlistResponse>>builder()
                .result(wishlistService.getWishlist())
                .build();
    }

    @PostMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> addToWishlist(@PathVariable Long courseId) {
        wishlistService.addToWishlist(courseId);
        return ApiResponse.<Void>builder().message("Course added to wishlist successfully").build();
    }

    @GetMapping("/{courseId}/exists")
    public ApiResponse<Boolean> isInWishlist(@PathVariable Long courseId) {
        boolean exists = wishlistService.isInWishlist(courseId);
        return ApiResponse.<Boolean>builder().result(exists).build();
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> removeFromWishlist(@PathVariable Long courseId) {
        wishlistService.removeFromWishlist(courseId);
        return ApiResponse.<Void>builder()
                .message(i18nService.msg("msg.remove.course.wishlist"))
                .build();
    }
}
