package com.ouyang.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ouyang.coffee.Coffee;
import com.ouyang.coffee.CoffeeRepository;
import com.ouyang.mvc.model.TradeCoffee;
import com.ouyang.mvc.model.TradeRequest;
import com.ouyang.mvc.model.TradeResponse;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository trasactionRepository;

    @Autowired
    private CoffeeRepository goodsRepository;

    @Transactional(rollbackFor = Exception.class)
    public TradeResponse trade(TradeRequest tradeRequest) {

        Transaction transaction = this.saveTransactionAndTradeGoods(tradeRequest);
        return this.createTradeResponse(transaction);

    }

    private Transaction saveTransactionAndTradeGoods(TradeRequest tradeRequest) {

        Transaction transaction = new Transaction();
        transaction.setTradeDate(new Date());
        transaction.setCustomerId(tradeRequest.getCustomerId());
        transaction.setBranchId(tradeRequest.getBranchId());
        transaction.setTotalPrice(tradeRequest.getTotalPrice());
        transaction.setItems(this.createTrasactionItems(transaction, tradeRequest.getTradeList()));
        return trasactionRepository.save(transaction);

    }

    private List<TransactionItem> createTrasactionItems(Transaction transaction, List<TradeCoffee> buyingList) {

        List<TransactionItem> trasactionItems = new ArrayList<>();

        for (TradeCoffee buyingGoods : buyingList) {

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setCoffeeId(buyingGoods.getCoffeeId());
            transactionItem.setAmount(buyingGoods.getAmount());

            trasactionItems.add(transactionItem);

        }

        return trasactionItems;

    }

    private TradeResponse createTradeResponse(Transaction transaction) {

        TradeResponse tradeResponse = new TradeResponse();
        tradeResponse.setTradeId(transaction.getId());
        tradeResponse.setTradeDate(transaction.getTradeDate());
        tradeResponse.setTotalPrice(transaction.getTotalPrice());
        tradeResponse.setTradeList(this.createTradeListAfterSave(transaction.getItems()));

        return tradeResponse;

    }

    private List<TradeCoffee> createTradeListAfterSave(List<TransactionItem> transactionItems) {

        List<TradeCoffee> buyingGoodsList = new ArrayList<>();

        for (TransactionItem transactionItem : transactionItems) {

            if (transactionItem.getAmount() <= 0) {

                continue;

            }

            Coffee coffee = goodsRepository.findById(transactionItem.getCoffeeId()).get();

            TradeCoffee buyingGoods = new TradeCoffee();
            buyingGoods.setCoffeeId(transactionItem.getCoffeeId());
            buyingGoods.setCoffeeName(coffee.getName());
            buyingGoods.setAmount(transactionItem.getAmount());
            buyingGoods.setPrice(coffee.getPrice());
            buyingGoods.setSubtotal(coffee.getPrice().multiply(new BigDecimal(transactionItem.getAmount())));

            buyingGoodsList.add(buyingGoods);

        }

        return buyingGoodsList;

    }

    public Transaction queryTrade(Long tradeId) {

        try {

            return trasactionRepository.findById(tradeId).get();

        } catch (NoSuchElementException e) {

            throw new NoSuchElementException("Trade not exist!");

        }

    }

    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public TradeResponse doReturn(HttpSession session) {

        BigDecimal totalReturningPrice = (BigDecimal) session.getAttribute("totalReturningPrice");
        List<TradeCoffee> returningList = (List<TradeCoffee>) session.getAttribute("returningList");

        Transaction transaction = (Transaction) session.getAttribute("transaction");
        transaction.setTotalPrice(transaction.getTotalPrice().subtract(totalReturningPrice));
        List<TransactionItem> items = transaction.getItems();
        returningList.forEach(returnsItem -> {

            TransactionItem item = items.stream().filter(tmpItem -> tmpItem.getCoffeeId().equals(returnsItem.getCoffeeId())).findFirst().get();
            item.setAmount(item.getAmount() - returnsItem.getAmount());

        });

        transaction = trasactionRepository.save(transaction);
        return this.createTradeResponse(transaction);

    }

}
