package com.picpaysimplificado.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;

@Service
public class TransactionService {
  @Autowired
  private UserService userService;
  @Autowired
  private NotificationService notificationService;

  @Autowired
  private TransactionRepository repository;
  @Autowired
  private RestTemplate restTemplate;

  public Transaction createTransaction(TransactionDTO transaction) throws Exception {
    User sender = this.userService.findUserById(transaction.senderId());
    User receiver = this.userService.findUserById(transaction.receiverId());
    userService.validateTransaction(sender, transaction.value());

    boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
    if (!isAuthorized) {
      throw new Exception("Transação não autorizada");
    }

    Transaction newTransaction = new Transaction();
    newTransaction.setAmount(transaction.value());
    newTransaction.setSender(sender);
    newTransaction.setReceiver(receiver);
    newTransaction.setTimestamp(LocalDateTime.now());

    sender.setBalance(sender.getBalance().subtract(transaction.value()));
    receiver.setBalance(receiver.getBalance().add(transaction.value()));

    repository.save(newTransaction);
    userService.saveUser(sender);
    userService.saveUser(receiver);

    notificationService.sendNotification(sender, "Transação realizada com sucesso");
    notificationService.sendNotification(receiver, "Transação recebida com sucesso");

    return newTransaction;
  }

  public boolean authorizeTransaction(User sender, BigDecimal value) {
    ResponseEntity<Map> autorizationResponse = this.restTemplate.getForEntity(
      "https://run.mocky.io/v3/5794d450-d2e2-4412-8131-73d0293ac1cc",
      Map.class
    );

    if (autorizationResponse.getStatusCode() == HttpStatus.OK) {
      String message = (String) autorizationResponse.getBody().get("message");

      return "Autorizado".equalsIgnoreCase(message);
    }

    return false;
  }  
}
