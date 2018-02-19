package co.com.uniandes.quotationmicroservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.com.uniandes.quotationmicroservice.model.Quotation;
import co.com.uniandes.quotationmicroservice.repository.QuotationRepository;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
public class QuotationController {
	@Autowired
	QuotationRepository quotationRepository;

	// Get All Quotations
	@GetMapping("/quotations")
	public List<Quotation> getAllNotes() {
		return quotationRepository.findAll();
	}

	// Create a new Quotation
	@PostMapping("/quotations")
	public Quotation createNote(@Valid @RequestBody Quotation note) {
		return quotationRepository.save(note);
	}

	// Get a Single Quotation
	@GetMapping("/quotations/{id}")
	public ResponseEntity<Quotation> getNoteById(@PathVariable(value = "id") Long quotationId) {
		Quotation quotation = quotationRepository.findOne(quotationId);
		if (quotation == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().body(quotation);
	}

	// Update a Quotation
	@PutMapping("/quotations/{id}")
	public ResponseEntity<Quotation> updateNote(@PathVariable(value = "id") Long quotationId,
			@Valid @RequestBody Quotation quotationDetails) {
		Quotation quotation = quotationRepository.findOne(quotationId);
		if (quotation == null) {
			return ResponseEntity.notFound().build();
		}
		quotation.setIdUser(quotationDetails.getIdUser());
		quotation.setPrice(quotationDetails.getPrice());
		quotation.setProducts(quotationDetails.getProducts());

		Quotation updatedQuotation = quotationRepository.save(quotation);
		return ResponseEntity.ok(updatedQuotation);
	}

	// Delete a Quotation
	@DeleteMapping("/quotations/{id}")
	public ResponseEntity<Quotation> deleteNote(@PathVariable(value = "id") Long quotationId) {
		Quotation quotation = quotationRepository.findOne(quotationId);
		if (quotation == null) {
			return ResponseEntity.notFound().build();
		}

		quotationRepository.delete(quotation);
		return ResponseEntity.ok().build();
	}
}
