package br.com.minhagibiteca.finance.application;
import br.com.minhagibiteca.finance.application.FinanceView.*;
import br.com.minhagibiteca.finance.domain.FinanceMath;
import br.com.minhagibiteca.finance.infrastructure.FinanceRepository;
import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FinanceService {
 private final FinanceRepository repo;
 public FinanceService(FinanceRepository repo){this.repo=repo;}
 @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Report report(UUID owner,FinanceFilter f,int page){return report(owner,f,page,false);}
 @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Report report(UUID owner,FinanceFilter f,int page,boolean quantity){
  if(f.start()!=null && f.start().isBefore(LocalDate.of(1900,1,1)))throw new FinanceFilter.InvalidFilter("A data inicial deve ser a partir de 1900.");
  repo.requireOption(owner,"series",f.seriesId());repo.requireOption(owner,"creator",f.creatorId());
  repo.requireOption(owner,"character",f.characterId());repo.requireOption(owner,"publisher",f.publisherId());
  Summary summary=repo.summary(owner,f),coverage=repo.summary(owner,f.period(null,null));
  var charts=new ArrayList<Chart>();
  String[] keys={"seriesId","creatorId","characterId","publisherId","mediaType"};
  String[] titles={"Coleções / séries","Criadores","Personagens","Editoras","Físicos e digitais"};
  for(int k=0;k<keys.length;k++){
   boolean shared=k==1||k==2,ring=k==3||k==4;
   boolean byQuantity=quantity&&!ring;
   var raw=repo.groups(owner,f,keys[k],byQuantity);boolean truncated=raw.size()>10;
   var rows=new ArrayList<>(raw.stream().limit(10).toList());
   if(ring&&truncated){
    BigDecimal selected=rows.stream().map(Bucket::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
    long selectedCount=rows.stream().mapToLong(Bucket::count).sum();
    rows.add(new Bucket(null,"Outras editoras",summary.count()-selectedCount,summary.total().subtract(selected)));
   }
   BigDecimal max=rows.stream().map(b->byQuantity?BigDecimal.valueOf(b.count()):b.amount()).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
   List<ChartItem> items=new ArrayList<>();double offset=0;
   for(var b:rows){var share=FinanceMath.ratio(b.amount(),summary.total());items.add(new ChartItem(b,FinanceMath.ratio(byQuantity?BigDecimal.valueOf(b.count()):b.amount(),max),share,offset));offset+=share.doubleValue();}
   charts.add(new Chart(titles[k],keys[k],List.copyOf(items),shared,ring,truncated,byQuantity));
  }
  LocalDate end=f.end()==null?LocalDate.now():f.end(),start=f.start()==null?end.withDayOfMonth(1).minusMonths(11):f.start();
  var rawMonths=repo.months(owner,f.period(start,end));var byMonth=new HashMap<String,Bucket>();rawMonths.forEach(b->byMonth.put(b.key(),b));
  List<Bucket> months=new ArrayList<>();
  for(YearMonth m=YearMonth.from(start);!m.isAfter(YearMonth.from(end));m=m.plusMonths(1)){
   String key=m.toString();var b=byMonth.get(key);
   months.add(new Bucket(key,m.format(DateTimeFormatter.ofPattern("MMM/yyyy",Locale.forLanguageTag("pt-BR"))),b==null?0:b.count(),b==null?BigDecimal.ZERO:b.amount()));
  }
  BigDecimal max=months.stream().map(Bucket::amount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
  var points=new StringJoiner(" ");
  for(int i=0;i<months.size();i++){double x=20+680.0*i/Math.max(1,months.size()-1);double y=190-FinanceMath.ratio(months.get(i).amount(),max).doubleValue()*1.6;points.add(String.format(Locale.ROOT,"%.2f,%.2f",x,y));}
  if(months.size()==1)points.add("700.00,"+String.format(Locale.ROOT,"%.2f",190-FinanceMath.ratio(months.getFirst().amount(),max).doubleValue()*1.6));
  String peak=max.signum()==0?"Sem gastos monetários no período do gráfico":months.stream().filter(b->b.amount().compareTo(max)==0).findFirst().map(b->b.label()+" — "+b.getMoney()+" (primeiro mês em caso de empate)").orElse("");
  String range=start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" a "+end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
  Comparison comparison=null;
  if(f.start()!=null){
   LocalDate previousEnd=f.start().minusDays(1),previousStart=FinanceMath.previousStart(f.start(),f.end());
   var previous=repo.summary(owner,f.period(previousStart,previousEnd));
   BigDecimal largest=summary.total().max(previous.total());
   comparison=new Comparison(previous,previousStart.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" a "+previousEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    summary.total().subtract(previous.total()),FinanceMath.change(summary.total(),previous.total()),FinanceMath.ratio(summary.total(),largest),FinanceMath.ratio(previous.total(),largest));
  }
  return new Report(summary,coverage,repo.list(owner,f,page,summary.count()),List.copyOf(charts),new Monthly(List.copyOf(months),points.toString(),range,peak),comparison);
 }
 @Transactional(readOnly=true) public List<Option> options(UUID owner,String kind){return repo.options(owner,kind);}
 @Transactional(readOnly=true) public Purchase find(UUID owner,UUID id){return repo.find(owner,id);}
 @Transactional public UUID save(UUID owner,UUID id,Command c){
  repo.lock(owner);if(id!=null)repo.find(owner,id);
  repo.requireOption(owner,"edition",c.editionId());
  if(c.editionId()==null||c.amount()==null||c.amount().signum()<0||c.amount().scale()>2||c.amount().compareTo(new BigDecimal("9999999999.99"))>0)throw new InvalidPurchase("Informe edição e valor válido, não negativo, com até duas casas decimais.");
  if(c.purchaseDate()!=null&&(c.purchaseDate().isAfter(LocalDate.now())||c.purchaseDate().isBefore(LocalDate.of(1900,1,1))))throw new InvalidPurchase("Informe a data real da compra, entre 1900 e hoje, ou deixe em branco.");
  if((c.vendor()!=null&&c.vendor().length()>160)||(c.notes()!=null&&c.notes().length()>2000))throw new InvalidPurchase("Respeite os limites de origem e observações.");
  UUID target=id==null?UUID.randomUUID():id;repo.save(owner,target,c.editionId(),c.amount(),c.purchaseDate(),blank(c.vendor()),blank(c.notes()),id!=null);return target;
 }
 @Transactional public void delete(UUID owner,UUID id){repo.lock(owner);repo.find(owner,id);repo.delete(owner,id);}
 private String blank(String s){return s==null||s.isBlank()?null:s.strip();}
 public record Command(UUID editionId,BigDecimal amount,LocalDate purchaseDate,String vendor,String notes){}
 public static class InvalidPurchase extends RuntimeException{public InvalidPurchase(String m){super(m);}}
}
