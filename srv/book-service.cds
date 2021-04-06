using { my.bookshop as my } from '../db/schema';

service BookService {
    entity Books  as projection on my.Books;
}