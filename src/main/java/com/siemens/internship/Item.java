package com.siemens.internship;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String status;

    /*
    (?!.*\.\.) - prevents two dots from appearing anywhere in the email
    [a-zA-Z0-9._%+-]+ - allows letters and digits and common special characters. Must be at least one character
    @ - the required separator between the local part and domain
    [a-zA-Z0-9.-]+ - for the domain, allows letters, digits, dots and hyphens
    \. - mathces the dot before Top-Level-Domain
    [a-zA-Z]{2,} - matches the TLD. Must be at least two letters, only letters allowed
    */
    @Pattern(
            regexp = "^(?!.*\\.\\.)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email not valid"
    )
    private String email;


}