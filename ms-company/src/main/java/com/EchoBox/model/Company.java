package com.EchoBox.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "idCompany")
    private Integer id;

    @NotBlank
    @Column(name = "nameCompany", nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(name = "emailCompany", nullable = false)
    private String email;

    @NotBlank
    @Column(name = "cnpjCompany", nullable = false, length = 14)
    private String cnpj;
}
