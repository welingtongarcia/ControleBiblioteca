package br.com.minhagibiteca.finance.application;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import br.com.minhagibiteca.catalog.domain.MediaType;
public record FinanceFilter(LocalDate start,LocalDate end,UUID seriesId,UUID creatorId,UUID characterId,UUID publisherId,MediaType mediaType,boolean unknownMedia) {
 public FinanceFilter(LocalDate start,LocalDate end,UUID seriesId,UUID creatorId,UUID characterId,UUID publisherId,MediaType mediaType){this(start,end,seriesId,creatorId,characterId,publisherId,mediaType,false);}
 public FinanceFilter {
  if ((start==null)!=(end==null)) throw new InvalidFilter("Informe as duas datas do período ou deixe ambas vazias.");
  if(start!=null && (start.isAfter(end) || end.isAfter(LocalDate.now()) || ChronoUnit.DAYS.between(start,end)>3659))
   throw new InvalidFilter("Informe um período entre 1900 e hoje, em ordem, com até 3.660 dias.");
 }
 public FinanceFilter period(LocalDate from,LocalDate to) { return new FinanceFilter(from,to,seriesId,creatorId,characterId,publisherId,mediaType,unknownMedia); }
 public static class InvalidFilter extends RuntimeException { public InvalidFilter(String message){super(message);} }
}
