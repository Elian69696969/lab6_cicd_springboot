package ec.edu.espe.lab6_ci.service;

import ec.edu.espe.lab6_ci.dto.WalletResponse;
import ec.edu.espe.lab6_ci.model.Wallet;
import ec.edu.espe.lab6_ci.repository.WalletRepository;

import java.util.Optional;

// Asegúrate de importar tu RiskClient si está en otro paquete
// import ec.edu.espe.lab6_ci.client.RiskClient;

public class WalletService {
    private final WalletRepository walletRepository;
    private final RiskClient riskClient;

    public WalletService(WalletRepository walletRepository, RiskClient riskClient) {
        this.walletRepository = walletRepository;
        this.riskClient = riskClient;
    }

    // Crear billetera
    public WalletResponse createWallet(String ownerEmail, double initialBalance) {
        // Validaciones (Corregida la validación del '@')
        if (ownerEmail == null || !ownerEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Invalid balance must be >= 0");
        }

        // Regla de negocio: usuario bloqueado
        if (riskClient.isBlocked(ownerEmail)) {
            throw new IllegalStateException("User blocked");
        }

        // Regla de negocio: no duplicar billetera por email
        if (walletRepository.existsByOwnerEmail(ownerEmail)) {
            throw new IllegalStateException("Wallet already exists");
        }

        // Guardar y retornar
        Wallet wallet = new Wallet(ownerEmail, initialBalance);
        Wallet saved = walletRepository.save(wallet);

        return new WalletResponse(saved.getId(), saved.getBalance());
    }

    //Depositar dinero
    public double deposit(String walletId, double amount) {
        if(amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be > 0");
        }

        Optional<Wallet> found =  walletRepository.findById(walletId);
        if(found.isEmpty()){
            throw new IllegalArgumentException("Wallet not found");
        }

        Wallet wallet = found.get();
        wallet.deposit(amount);

        //Persistencia a la BDD
        walletRepository.save(wallet);

        return wallet.getBalance();
    }
}