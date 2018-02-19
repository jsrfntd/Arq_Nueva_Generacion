package co.com.uniandes.quotationmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import co.com.uniandes.quotationmicroservice.model.Quotation;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

}
