namespace my.bookshop;

using {
    my.bookshop as my,
    sap,
    managed
} from '@sap/cds/common';

@fiori.draft.enabled
entity Books:  managed {
    key bookId       : Integer;
    title        : String(111);
    descr        : String(1111);
    stock        : Integer;
    price        : Integer;
    rating       : Integer;
    amount       : Integer;
} 