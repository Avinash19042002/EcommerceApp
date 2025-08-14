package com.ecommerce.project.payload;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.modelmapper.internal.bytebuddy.dynamic.loading.InjectionClassLoader;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;
    @NotBlank
    @Size(min = 5,message = "CategoryName must contains Atleast 5 Characters")
    private String categoryName;
}
