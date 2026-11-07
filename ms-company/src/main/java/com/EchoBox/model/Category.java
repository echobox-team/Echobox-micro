package com.EchoBox.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "idCategory")
    private Integer id;

    @NotBlank
    @Column(name = "typeCategory", nullable = false)
    private String type;

    @NotBlank
    @Column(name = "colorCategory")
    private String color;
}
