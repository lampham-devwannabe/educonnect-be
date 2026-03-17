package com.sep.educonnect.service;

import com.sep.educonnect.dto.tutor.request.SubmitDocumentRequest;
import com.sep.educonnect.dto.tutor.request.UpdateDocumentRequest;
import com.sep.educonnect.entity.TutorDocument;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.enums.DocumentStatus;
import com.sep.educonnect.enums.DocumentType;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.TutorDocumentRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import com.sep.educonnect.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorDocumentService {
    final TutorProfileRepository tutorProfileRepository;
    final TutorDocumentRepository tutorDocumentRepository;
    final UserRepository userRepository;

    public List<TutorDocument> getSubmitDocument() {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return tutorDocumentRepository.findByProfileUserUserId(user.getUserId());
    }

    public TutorDocument submitTutorDocument(SubmitDocumentRequest request) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var tutorProfile =
                tutorProfileRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        String documentTypeStr = request.getDocumentType();
        if (documentTypeStr == null || documentTypeStr.isBlank()) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }

        if (request.getFileId() == null
                || request.getFileId().isBlank()
                || request.getFileId().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_DOCUMENTS);
        }

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(documentTypeStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }

        TutorDocument tutorDocument =
                TutorDocument.builder()
                        .fileName(request.getFileId())
                        .type(documentType)
                        .uploadedAt(LocalDateTime.now())
                        .status(DocumentStatus.PENDING)
                        .profile(tutorProfile)
                        .build();

        return tutorDocumentRepository.save(tutorDocument);
    }

    public TutorDocument updateTutorDocument(UpdateDocumentRequest request) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var tutorProfile =
                tutorProfileRepository
                        .findByUserUserId(
                                user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        if (request.getFileId() == null
                || request.getFileId().isBlank()
                || request.getFileId().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_DOCUMENTS);
        }

        String documentTypeStr = request.getDocumentType();
        if (documentTypeStr == null || documentTypeStr.isBlank()) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(documentTypeStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }

        var document =
                tutorDocumentRepository
                        .findById(request.getDocumentId())
                        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        document.setFileName(request.getFileId());
        document.setStatus(DocumentStatus.PENDING);
        return tutorDocumentRepository.save(document);
    }
}
