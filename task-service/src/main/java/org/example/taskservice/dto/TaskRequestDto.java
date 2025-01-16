package org.example.taskservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskRequestDto {

    private String name;
    private String description;
}
