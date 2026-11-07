package com.EchoBox.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "\"status\"")
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "idStatus")
    private Integer id;

    @NotBlank
    @Column(name = "typeStatus", nullable = false)
    private String type;

    @Column(name = "colorStatus")
    private String color;
}
