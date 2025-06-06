package com.codewithzea.projecttrackingsystem.dto;


import lombok.*;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ProjectRedisDTO {

    private Long id;
    private String name;
    private String description;
    private LocalDate deadline;
    private String status;
}

