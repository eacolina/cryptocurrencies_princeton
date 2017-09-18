package scroogeCoin;

import java.util.ArrayList;


public class TxHandler {

    /**
     * Creates a public ledger whose current scroogeCoin.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the scroogeCoin.UTXOPool(scroogeCoin.UTXOPool uPool)
     * constructor.
     */

    private UTXOPool currentPool;
    public TxHandler(UTXOPool utxoPool) {
        this.currentPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current scroogeCoin.UTXO pool, (i.e origin output)
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no scroogeCoin.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */

    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUTXOs = new UTXOPool();
        double currentInputSum = 0;
        double currentOutputSum = 0;

        for(int i = 0; i < tx.numInputs(); i++){ // this checks if the incoming bitcoins are coming from a valid UTXO (requirement 1)
            Transaction.Input currentInput = tx.getInput(i);
            UTXO currentUTXO = new UTXO(currentInput.prevTxHash, currentInput.outputIndex);
            Transaction.Output originOutput = currentPool.getTxOutput(currentUTXO);
            if(!currentPool.contains(currentUTXO)) return false;
            if(!Crypto.verifySignature(originOutput.address,tx.getRawDataToSign(i),currentInput.signature)) { // check signatures
                return false;
            }
            currentInputSum += originOutput.value;
            if(uniqueUTXOs.contains(currentUTXO)) return false;
            uniqueUTXOs.addUTXO(currentUTXO,originOutput);
            currentPool.removeUTXO(currentUTXO);
        }
        for(Transaction.Output out: tx.getOutputs()){ // this checks if the incoming bitcoins are coming from a valid UTXO (requirement 1)
            if(out.value < 0) return false;
            currentOutputSum += out.value;
        }
        if(currentOutputSum > currentInputSum) return false;
       return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current scroogeCoin.UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        ArrayList<Transaction> validTransactions;
        validTransactions = new ArrayList<>(possibleTxs.length);

        for(Transaction tx: possibleTxs){
            if(isValidTx(tx)) {
                validTransactions.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO removeUTXO = new UTXO(in.prevTxHash, in.outputIndex);
                    currentPool.removeUTXO(removeUTXO);
                }
                for (int i = 0; i <= tx.getOutputs().size(); i++) {
                    Transaction.Output currentOutput = tx.getOutput(i);
                    UTXO toAddUTXO = new UTXO(tx.getHash(), i);
                    currentPool.addUTXO(toAddUTXO, currentOutput);
                }
            }
        }
        return validTransactions.toArray(new Transaction[0]);
    }

}
