package com.EchoBox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "idUser")
    private Integer id;

    @NotBlank
    @Column(name = "emailUser", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "passwordUser", nullable = false, length = 64)
    private String password;

    @Column(name = "pictureUser")
    private String picture;

    @Column(name = "isAdminUser", nullable = false)
    private Boolean isAdmin = false;

    /**
     * In the microservices architecture, Company belongs to ms-company.
     * We store only the logical foreign key ID (fk_user_idCompany) to maintain loose coupling!
     */
    @JsonProperty("company")
    @Column(name = "fk_user_idCompany")
    private Integer companyId;
}
