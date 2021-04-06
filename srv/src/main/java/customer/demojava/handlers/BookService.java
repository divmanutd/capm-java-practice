package customer.testjava.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cds.gen.my.bookshop.Books;
import cds.gen.my.bookshop.Books_;


@Component
@ServiceName("BookService")
public class BookService implements EventHandler {
    
    @Autowired
PersistenceService db;

private final CqnAnalyzer analyzer;

    private Map<Object, Map<String, Object>> books = new HashMap<>();

    BookService( CdsModel model) {
		this.analyzer = CqnAnalyzer.create(model);
	}

     @On(event = CdsService.EVENT_CREATE, entity = "BookService.Books")
     public void onCreate(CdsCreateEventContext context) {
         context.getCqn().entries().forEach(e -> books.put(e.get("BOOK_ID"), e));
         context.setResult(context.getCqn().entries());
     }

     @On(event = CdsService.EVENT_READ, entity = "BookService.Books")
     public void onRead(CdsReadEventContext context) {
         context.setResult(books.values());
     }

     @On(event = CdsService.EVENT_UPDATE, entity = "BookService.Books")
     public void validateBookAndDecreaseStock(List<Books> items) {
        for (Books item : items) {
            Integer bookId = item.getBookId();
            Integer amount = item.getAmount();

            // check if the book that should be ordered is existing
            CqnSelect sel = Select.from(Books_.class).columns(b -> b.stock()).where(b -> b.bookId().eq(bookId));
            Books book = db.run(sel).first(Books.class)
                    .orElseThrow(() -> new ServiceException(ErrorStatuses.NOT_FOUND, "Book does not exist"));

            // check if order could be fulfilled
            int stock = book.getStock();
            if (stock < amount) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Not enough books on stock");
            }

            // update the book with the new stock, means minus the order amount
            book.setStock(stock - amount);
            CqnUpdate update = Update.entity(Books_.class).data(book).where(b -> b.bookId().eq(bookId));
            db.run(update);
        }
    }

     @On(event = CdsService.EVENT_DELETE)
     public void deleteBook(CdsDeleteEventContext context) {
        
            Integer id = (Integer) analyzer.analyze(context.getCqn()).targetKeys().get(Books.BOOK_ID);
            CqnDelete delete = Delete.from(Books_.class).where(b -> b.bookId().eq(id));
            db.run(delete);
    }
}
