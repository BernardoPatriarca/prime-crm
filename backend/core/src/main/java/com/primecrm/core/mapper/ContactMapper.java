package com.primecrm.core.mapper;

import com.primecrm.core.dto.commercial.ContactRequest;
import com.primecrm.core.dto.commercial.ContactResponse;
import com.primecrm.infra.entity.commercial.Contact;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface ContactMapper {

    ContactResponse toResponse(Contact contact);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "primaryContact", ignore = true)
    @Mapping(target = "decisionMaker", ignore = true)
    Contact toEntity(ContactRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "primaryContact", ignore = true)
    @Mapping(target = "decisionMaker", ignore = true)
    void updateEntity(@MappingTarget Contact contact, ContactRequest request);
}
