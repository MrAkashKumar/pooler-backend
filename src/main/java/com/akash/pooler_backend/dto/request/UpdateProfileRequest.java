package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min=2,max=100)
    private String firstName;
    @Size(min=2,max=100)
    private String lastName;
    private String profilePictureUrl;
}
