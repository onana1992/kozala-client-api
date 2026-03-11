package com.neobank.kozala_client.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 150)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150)
    private String lastName;

    @Size(max = 20)
    private String gender;

    private LocalDate birthDate;

    @Size(max = 20)
    private String maritalStatus;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String region;

    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String fullAddress;
}
