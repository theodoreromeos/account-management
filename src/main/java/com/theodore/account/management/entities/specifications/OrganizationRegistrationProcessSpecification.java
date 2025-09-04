package com.theodore.account.management.entities.specifications;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrganizationRegistrationProcessSpecification {

    private static final String STATUS = "adminApprovedStatus";

    private OrganizationRegistrationProcessSpecification() {
    }

    public static Specification<OrganizationRegistrationProcess> filterCriteria(SearchRegistrationProcessRequestDto searchRequest) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ObjectUtils.isEmpty(searchRequest.organizationName())) {
                predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("organizationName"), searchRequest.organizationName())));
            }
            if (!ObjectUtils.isEmpty(searchRequest.country())) {
                predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("country"), searchRequest.country())));
            }
            if (!ObjectUtils.isEmpty(searchRequest.registrationNumber())) {
                predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("registrationNumber"), searchRequest.registrationNumber())));
            }
            switch (searchRequest.status()) {
                case ALL:
                    break;
                case APPROVED:
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get(STATUS), OrganizationRegistrationStatus.APPROVED)));
                    break;
                case PENDING:
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get(STATUS), OrganizationRegistrationStatus.PENDING)));
                    break;
                case REJECTED:
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get(STATUS), OrganizationRegistrationStatus.REJECTED)));
                    break;
            }
            if (criteriaQuery != null) {
                criteriaQuery.distinct(true);
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

}
