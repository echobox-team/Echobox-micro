package com.EchoBox.config;

import com.EchoBox.model.Category;
import com.EchoBox.model.Company;
import com.EchoBox.model.Status;
import com.EchoBox.repository.CategoryRepository;
import com.EchoBox.repository.CompanyRepository;
import com.EchoBox.repository.StatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initCompanyData(
            CompanyRepository companyRepo,
            CategoryRepository categoryRepo,
            StatusRepository statusRepo) {
        return args -> {
            if (categoryRepo.count() == 0) {
                createCategory(categoryRepo, "Qualidade do Produto", "#4CAF50");
                createCategory(categoryRepo, "Atendimento ao Cliente", "#2196F3");
                createCategory(categoryRepo, "Usabilidade do Site", "#FF9800");
                createCategory(categoryRepo, "Velocidade de Entrega", "#9C27B0");
                createCategory(categoryRepo, "Preços", "#F44336");
                createCategory(categoryRepo, "Relatório de Bug", "#795548");
                createCategory(categoryRepo, "Solicitação de Funcionalidade", "#607D8B");
                createCategory(categoryRepo, "Feedback Geral", "#9E9E9E");
            }

            if (statusRepo.count() == 0) {
                createStatus(statusRepo, "Pendente", "#FF9800");
                createStatus(statusRepo, "Em Andamento", "#2196F3");
                createStatus(statusRepo, "Resolvido", "#4CAF50");
                createStatus(statusRepo, "Fechado", "#9E9E9E");
                createStatus(statusRepo, "Rejeitado", "#F44336");
            }

            if (companyRepo.count() == 0) {
                createCompany(companyRepo, "EchoBox Solutions", "contato@echobox.com", "12345678000100");
                createCompany(companyRepo, "Tech Innovations Inc.", "info@techinnovations.com", "98765432000199");
                createCompany(companyRepo, "Serviços Globais Ltda.", "suporte@servicosglobais.com", "11223344000155");
                createCompany(companyRepo, "Negócios Locais Co.", "ola@negocioslocais.co", "55667788000122");
                createCompany(companyRepo, "Soluções Digitais", "contato@solucoesdigitais.com", "99887766000144");
                createCompany(companyRepo, "Cliente Primeiro Corp", "suporte@clienteprimeiro.com", "33445566000177");
            }
        };
    }

    private void createCategory(CategoryRepository repo, String type, String color) {
        Category c = new Category();
        c.setType(type);
        c.setColor(color);
        repo.save(c);
    }

    private void createStatus(StatusRepository repo, String type, String color) {
        Status s = new Status();
        s.setType(type);
        s.setColor(color);
        repo.save(s);
    }

    private void createCompany(CompanyRepository repo, String name, String email, String cnpj) {
        Company c = new Company();
        c.setName(name);
        c.setEmail(email);
        c.setCnpj(cnpj);
        repo.save(c);
    }
}
