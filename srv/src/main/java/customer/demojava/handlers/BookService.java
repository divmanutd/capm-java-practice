package customer.demojava.handlers;

import java.util.List;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cds.gen.bookservice.Books;
import cds.gen.my.bookshop.Books_;



@Component
@ServiceName("BookService")
public class BookService implements EventHandler {
    
@Autowired
PersistenceService db;

     @On(event = CdsService.EVENT_READ, entity = "BookService.Books")
     public void onAddBook() {
         System.out.println("On Read");
     }
   
     @After(event = CdsService.EVENT_READ, entity = "BookService.Books")
     public void addStock(List<Books> items) {
        for (Books item : items) {
            Integer bookId = item.getBookId();
            Integer amount = item.getAmount();

            // check if the book that should be ordered is existing
            CqnSelect sel = Select.from(Books_.class).columns(b -> b.stock()).where(b -> b.bookId().eq(bookId));
            cds.gen.my.bookshop.Books book = db.run(sel).first(cds.gen.my.bookshop.Books.class)
                    .orElseThrow(() -> new ServiceException(ErrorStatuses.NOT_FOUND, "Book does not exist"));

            int stock = book.getStock();
            // update the book with the new stock, means adding the new amount
            if (stock < 10) {
            book.setStock(stock + amount);
            CqnUpdate update = Update.entity(Books_.class).data(book).where(b -> b.bookId().eq(bookId));
            db.run(update);
            }
        }
    }

    @Before(event = CdsService.EVENT_CREATE, entity = "BookService.Books")
     public void checkStockBeforeAddingBook(List<Books> items) {
        for (Books item : items) {
            Integer bookId = item.getBookId();
            Integer amount = item.getAmount();

            // check if the book that should be ordered is existing
            CqnSelect sel = Select.from(Books_.class).columns(b -> b.stock()).where(b -> b.bookId().eq(bookId));
            cds.gen.my.bookshop.Books book = db.run(sel).first(cds.gen.my.bookshop.Books.class)
                    .orElseThrow(() -> new ServiceException(ErrorStatuses.NOT_FOUND, "Book does not exist"));

            int stock = book.getStock();
            // update the book with the new stock, if there is no stock
            if (stock == 0) {
            book.setStock(stock + amount);
            CqnUpdate update = Update.entity(Books_.class).data(book).where(b -> b.bookId().eq(bookId));
            db.run(update);
            }
        }
    }
}
