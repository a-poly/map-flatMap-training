package exercise.bookstore.business.impl;


import errors.GenericError;
import errors.impl.MyError;
import exercise.bookstore.bean.*;
import exercise.bookstore.business.SummaryService;
import exercise.bookstore.service.ServiceAuthor;
import exercise.bookstore.service.ServiceBook;
import exercise.bookstore.service.ServiceChapter;
import exercise.bookstore.service.ServiceSales;
import monad.MonadFutEither;
import scala.concurrent.Future;
import scala.util.Either;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class SummaryServiceImpl implements SummaryService<GenericError> {

	private final ServiceBook<GenericError> srvBook;
	private final ServiceSales<GenericError> srvSales;
	private final ServiceChapter<GenericError> srvChapter;
	private final ServiceAuthor<GenericError> srvAuthor;
	
	private final MonadFutEither<GenericError> m;
	
	
	public SummaryServiceImpl(ServiceBook<GenericError> srvBook,
			ServiceSales<GenericError> srvSales,
			ServiceChapter<GenericError> srvChapter,
			ServiceAuthor<GenericError> srvAuthor,
			MonadFutEither<GenericError> m) {
		super();
		this.srvBook = srvBook;
		this.srvSales = srvSales;
		this.srvChapter = srvChapter;
		this.srvAuthor = srvAuthor;
		this.m = m;
	}

	@Override
	public Future<Either<GenericError, Summary>> getSummary(Integer idBook) {

		// Define all the futures
		Future<Either<GenericError, Book>> bookF = this.srvBook.getBook(idBook);
		Future<Either<GenericError, Optional<Sales>>> salesF = m.dslFrom(this.srvSales.getSales(idBook))
				.map(sales -> Optional.of(sales))
				.recover(error -> Optional.empty())
				.value();
		Future<Either<GenericError, Author>> authorF = m.flatMap(
				bookF,
				book -> this.srvAuthor.getAuthor(book.getIdAuthor())
		);
		Future<Either<GenericError, List<Chapter>>> chaptersF = m.flatMap(
				bookF,
				book -> m.sequence(
						book.getChapters().stream().map(
								chapterId -> this.srvChapter.getChapter(chapterId)
						).collect(Collectors.toList())
				)
		);

		// Combine them
		Future<Either<GenericError, Summary>> summaryF = m.map4(
				bookF,
				chaptersF,
				salesF,
				authorF,
				(book, chapters, sales, author) -> new Summary(book, chapters, sales, author)
		);

		return m.dslFrom(summaryF)
				.recoverWith(
						error -> m.raiseError(new MyError("It is impossible to get book summary"))
				).value();
	}
}
