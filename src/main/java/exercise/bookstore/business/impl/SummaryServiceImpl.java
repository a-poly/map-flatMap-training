package exercise.bookstore.business.impl;


import errors.GenericError;
import errors.impl.MyError;
import exercise.bookstore.bean.Author;
import exercise.bookstore.bean.Book;
import exercise.bookstore.bean.Chapter;
import exercise.bookstore.bean.Sales;
import exercise.bookstore.bean.Summary;
import exercise.bookstore.business.SummaryService;
import exercise.bookstore.service.ServiceAuthor;
import exercise.bookstore.service.ServiceBook;
import exercise.bookstore.service.ServiceChapter;
import exercise.bookstore.service.ServiceSales;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import monad.MonadFutEither;
import scala.concurrent.Future;
import scala.util.Either;


public class SummaryServiceImpl implements SummaryService<GenericError> {

	private final ServiceBook<GenericError> srvBook;
	private final ServiceSales<GenericError> srvSales;
	private final ServiceChapter<GenericError> srvChapter;
	private final ServiceAuthor<GenericError> srvAuthor;
	
	private final MonadFutEither<GenericError> monadTransformer;
	
	
	public SummaryServiceImpl(ServiceBook<GenericError> srvBook,
			ServiceSales<GenericError> srvSales,
			ServiceChapter<GenericError> srvChapter,
			ServiceAuthor<GenericError> srvAuthor,
			MonadFutEither<GenericError> monadTransformer) {
		super();
		this.srvBook = srvBook;
		this.srvSales = srvSales;
		this.srvChapter = srvChapter;
		this.srvAuthor = srvAuthor;
		this.monadTransformer = monadTransformer;
	}

	@Override
	public Future<Either<GenericError, Summary>> getSummary(Integer bookId) {


		//Get Book Future
		Future<Either<GenericError, Book>> bookF = this.srvBook.getBook(bookId);

		//Get Sales Future
		Future<Either<GenericError, Optional<Sales>>> salesF =
			this.monadTransformer
				.dslFrom(this.srvSales.getSales(bookId))
				.map(sales -> Optional.of(sales))
				.handleError(error -> Optional.empty())
				.value();

		//Get Author Future
		Future<Either<GenericError, Author>> authorF =
			this.monadTransformer.flatMap(
				bookF,
				book -> this.srvAuthor.getAuthor(book.getIdAuthor())
			);

		//Get Chapters Future
		Future<Either<GenericError, List<Chapter>>> chaptersF = this.monadTransformer.flatMap(
			bookF,
			book ->
				this.monadTransformer
					.sequence(
						book
							.getChapters()
							.stream()
							.map(chapterId -> this.srvChapter.getChapter(chapterId))
							.collect(Collectors.toList())
					)
		);

		return this.monadTransformer.dslFrom(bookF).map4(
			salesF,
			authorF,
			chaptersF,
			(book, sales, author, chapters) -> new Summary(book, chapters, sales, author)
		).handleErrorWith(
			error -> this.monadTransformer.raiseError(new MyError("It is impossible to get book summary"))
		).value();

	}
}
