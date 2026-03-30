package com.authservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_attr_value_user_attr", columnNames = {"user_id", "attribute_id"})
        },
        indexes = {
                @Index(name = "idx_user_attr_values_user_id", columnList = "user_id"),
                @Index(name = "idx_user_attr_values_attribute_id", columnList = "attribute_id"),
                @Index(name = "idx_user_attr_values_attribute_value", columnList = "attribute_id, value")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAttributeValueEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CustomAttributeDefinitionEntity attribute;

    @Column(name = "value", nullable = false)
    private String value;
}
