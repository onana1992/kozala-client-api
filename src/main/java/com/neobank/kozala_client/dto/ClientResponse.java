package com.neobank.kozala_client.dto;

import com.neobank.kozala_client.entity.ClientStatus;
import com.neobank.kozala_client.entity.ClientType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {

    private Long id;
    private ClientType type;
    private String displayName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private ClientStatus status;
    private Integer riskScore;
    private Boolean pepFlag;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
