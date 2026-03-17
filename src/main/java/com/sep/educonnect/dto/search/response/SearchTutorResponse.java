package com.sep.educonnect.dto.search.response;


import java.util.List;

public record SearchTutorResponse(List<TutorResponse> tutors, Pagination pagination, long timeTaken, 
                                 String query, List<String> filters) {
}
