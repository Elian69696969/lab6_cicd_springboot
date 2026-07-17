package ec.edu.espe.lab6_ci.service;

import ec.edu.espe.lab6_ci.dto.WalletResponse;
import ec.edu.espe.lab6_ci.model.Wallet;
import ec.edu.espe.lab6_ci.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Optional;

public class WalletServiceTest {

    private WalletService walletService;
    private RiskClient riskClient;
    private WalletRepository walletRepository;

    // Arrange común de todas las pruebas
    @BeforeEach
    public void setUp() {
        walletRepository = Mockito.mock(WalletRepository.class);
        riskClient = Mockito.mock(RiskClient.class);
        walletService = new WalletService(walletRepository, riskClient);
    }

    @Test
    void createWallet_validData_shouldSaveAndReturnResponse() {
        // Arrange
        String email = "luis.collaguazo@ejemplo.com";
        double balance = 150.00;

        Mockito.when(riskClient.isBlocked(email)).thenReturn(false);
        Mockito.when(walletRepository.existsByOwnerEmail(email)).thenReturn(false);
        Mockito.when(walletRepository.save(ArgumentMatchers.any(Wallet.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // Act
        WalletResponse response = walletService.createWallet(email, balance);

        // Assert
        Assertions.assertNotNull(response.getWalletId(), "El ID del Wallet no debe ser NULL");
        Assertions.assertEquals(150.00, response.getBalance());

        Mockito.verify(riskClient).isBlocked(email);
        Mockito.verify(walletRepository).existsByOwnerEmail(email);
        Mockito.verify(walletRepository).save(ArgumentMatchers.any(Wallet.class));
    }

    @Test
    void createWallet_invalidData_shouldThrowException_andNotCallDependencies() {
        // Arrange
        String invalid = "luis.collaguazo-ejemplo.com";
        double balance = 15.00;

        // Act + Assert
        Assertions.assertThrows(IllegalArgumentException.class, ()
                -> walletService.createWallet(invalid, balance));

        // No debe llamar a ninguna dependencia porque falla la validacion
        Mockito.verifyNoInteractions(walletRepository, riskClient);
    }

    @Test
    void deposit_walletNotFound_shouldThrowException(){
        String walletId = "no-existe";

        Mockito.when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act + Assert
        // Cambiamos IllegalStateException.class por IllegalArgumentException.class
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()
                -> walletService.deposit(walletId, 50));

        Assertions.assertEquals("Wallet not found", ex.getMessage());
        Mockito.verify(walletRepository).findById(walletId);
        Mockito.verify(walletRepository, Mockito.never()).save(ArgumentMatchers.any(Wallet.class));
    }

    @Test
    void deposit_shouldUpdateBalance_andSave_usingCaptor() {
        // Arrange
        Wallet wallet = new Wallet("collaguazo.elian@ejemplo.com", 100.00);
        String walletId = wallet.getId();

        Mockito.when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        Mockito.when(walletRepository.save(ArgumentMatchers.any(Wallet.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);

        // Act
        double newBalance = walletService.deposit(walletId, 30.00);

        // Assert
        Mockito.verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();
        Assertions.assertEquals(newBalance, saved.getBalance());
    }
}