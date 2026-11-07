package com.bank.creditbatch.controller;

import com.bank.creditbatch.entity.Client;
import com.bank.creditbatch.repository.ClientRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.bank.creditbatch.service.CsvClientImportService;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientRepository clientRepository;
    private final CsvClientImportService csvClientImportService;

    public ClientController(ClientRepository clientRepository,
                            CsvClientImportService csvClientImportService) {
        this.clientRepository = clientRepository;
        this.csvClientImportService = csvClientImportService;
    }

    @GetMapping("/clients")
    public List<Client> getClients() {
        return clientRepository.findAll();
    }

    @PostMapping("/clients/import")
    public ImportResult importClients(@RequestParam("file") MultipartFile file) {
        int imported = csvClientImportService.importFile(file);
        return new ImportResult(imported, "CSV imported and clients table replaced");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> handleInvalidCsv(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResult(exception.getMessage()));
    }

    public record ImportResult(int imported, String message) {}
    public record ErrorResult(String message) {}
}
