package com.chunshuiquan.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateMomentRequest {
    private String content;
    private List<String> imageUrls;
    private String location;
    private String visibility; // public / friends / private
}
