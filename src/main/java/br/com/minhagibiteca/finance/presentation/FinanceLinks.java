package br.com.minhagibiteca.finance.presentation;
import br.com.minhagibiteca.finance.application.FinanceFilter;
import org.springframework.web.util.UriComponentsBuilder;
public record FinanceLinks(FinanceFilter filter,boolean quantity){
 public String page(int page){return build(null,null,Math.max(0,page));}
 public String refine(String dimension,String value){if(!java.util.Set.of("seriesId","creatorId","characterId","publisherId","mediaType").contains(dimension))throw new IllegalArgumentException();return build(dimension,value,0);}
 private String build(String dimension,String value,int page){
  var b=UriComponentsBuilder.fromPath("/financeiro");
  Object[] values={filter.start(),filter.end(),filter.seriesId(),filter.creatorId(),filter.characterId(),filter.publisherId(),filter.unknownMedia()?"UNSPECIFIED":filter.mediaType(),quantity?"QUANTITY":"AMOUNT"};
  String[] names={"start","end","seriesId","creatorId","characterId","publisherId","mediaType","ranking"};
  for(int i=0;i<names.length;i++){Object v=names[i].equals(dimension)?value:values[i];if(v!=null)b.queryParam(names[i],v);}
  if(page>0)b.queryParam("page",page);return b.build().encode().toUriString();
 }
}
