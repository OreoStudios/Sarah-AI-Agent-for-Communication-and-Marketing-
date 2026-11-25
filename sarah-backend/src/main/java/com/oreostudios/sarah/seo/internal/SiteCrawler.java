package com.oreostudios.sarah.seo.internal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Component
public class SiteCrawler {
    private static final String AGENT = "SarahSEO";
    private final URI origin;
    private final int limit;
    public SiteCrawler(@Value("${SARAH_SEO_ORIGIN:https://oreostudios.fr}") String origin,
                       @Value("${SARAH_SEO_MAX_PAGES:100}") int limit) {
        URI u=URI.create(origin);
        if (!"https".equals(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null ||
            u.getPort()!=-1 || u.getQuery()!=null || u.getFragment()!=null ||
            !(u.getPath().isEmpty() || u.getPath().equals("/"))) throw new IllegalArgumentException("SEO origin must be an HTTPS origin");
        this.origin=URI.create("https://"+u.getHost().toLowerCase(Locale.ROOT));
        this.limit=Math.clamp(limit,1,500);
    }
    String host(){return origin.getHost();}
    URI normalize(String value, URI base) {
        try {
            URI u=base.resolve(value).normalize();
            if(!"https".equalsIgnoreCase(u.getScheme()) || !host().equalsIgnoreCase(u.getHost()) ||
               u.getUserInfo()!=null || u.getPort()!=-1 || u.getRawQuery()!=null) return null;
            String path=u.getRawPath();
            if(path==null || path.isEmpty()) path="/";
            String decoded=u.getPath().toLowerCase(Locale.ROOT);
            if(decoded.matches(".*(?:^|/)(admin|api|logout|login|signin|signout|checkout|cart|account|auth|delete|unsubscribe)(?:/|$).*")) return null;
            return URI.create(origin+path);
        } catch(RuntimeException ex){return null;}
    }
    private org.jsoup.Connection.Response fetch(URI u) throws Exception {
        if(normalize(u.toString(),origin)==null) throw new IllegalArgumentException("Outside crawl scope");
        for(InetAddress address:InetAddress.getAllByName(host())){
            byte[] b=address.getAddress();
            if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||
                address.isSiteLocalAddress()||address.isMulticastAddress()||
                (b.length==16&&(b[0]&0xfe)==0xfc)||(b.length==4&&(b[0]&255)==100&&(b[1]&192)==64))
                throw new IllegalArgumentException("Non-public address");
        }
        Thread.sleep(150);
        return Jsoup.connect(u.toString()).userAgent(AGENT+"/1.0").timeout(4000).maxBodySize(2_000_000)
            .followRedirects(false).ignoreHttpErrors(true).ignoreContentType(true).execute();
    }
    record Rule(String path,boolean allow) {}
    record Group(List<String> agents,List<Rule> rules){}
    static boolean allowed(String robots,String agent,String path){
        List<Group> groups=new ArrayList<>();
        List<String> agents=new ArrayList<>(); List<Rule> rules=new ArrayList<>();
        for(String raw:robots.split("\\R")){
            String line=raw.split("#",2)[0].strip(); int i=line.indexOf(':'); if(i<0)continue;
            String k=line.substring(0,i).strip().toLowerCase(Locale.ROOT),v=line.substring(i+1).strip();
            if(k.equals("user-agent")){
                if(!rules.isEmpty()){groups.add(new Group(agents,rules));agents=new ArrayList<>();rules=new ArrayList<>();}
                agents.add(v.toLowerCase(Locale.ROOT));
            } else if(!agents.isEmpty()&&(k.equals("allow")||k.equals("disallow"))&&!v.isEmpty())rules.add(new Rule(v,k.equals("allow")));
        }
        if(!agents.isEmpty())groups.add(new Group(agents,rules));
        int specificity=-1; List<Rule> selected=new ArrayList<>();
        for(Group g:groups) for(String a:g.agents()){
            int n=a.equals("*")?0:agent.toLowerCase(Locale.ROOT).startsWith(a)?a.length():-1;
            if(n<0)continue; if(n>specificity){specificity=n;selected.clear();}
            if(n==specificity)selected.addAll(g.rules());
        }
        int best=-1;boolean allow=true;
        for(Rule rule:selected){
            String r=rule.path();boolean end=r.endsWith("$");if(end)r=r.substring(0,r.length()-1);
            String regex=Arrays.stream(r.split("\\*",-1)).map(Pattern::quote).reduce((a,b)->a+".*"+b).orElse("");
            int length=r.replace("*","").length();
            if(Pattern.compile("^"+regex+(end?"$":".*")).matcher(path).matches() &&
              (length>best || (length==best&&rule.allow()))){best=length;allow=rule.allow();}
        }
        return allow;
    }
    public record Page(String url,int status,String title,String description,String canonical,int h1,int h2,int h3,
        int words,int missingAlt,int internalLinks,int externalLinks,boolean structuredData,boolean noindex,
        String redirect,String fingerprint,List<String> issues,List<String> links){}
    public record Report(String origin,Instant at,int pages,List<Page> results,Map<String,Boolean> aiRootAccess,
                         List<String> notes,boolean complete){}
    private static String digest(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    static Page inspect(URI u,int status,String html,String headerRobots,String redirect,SiteCrawler crawler) throws Exception {
        Document d=Jsoup.parse(html,u.toString());List<String> issues=new ArrayList<>();
        String title=d.title(),desc=d.select("meta[name=description]").attr("content"),canonical=d.select("link[rel=canonical]").attr("href");
        int h1=d.select("h1").size(),h2=d.select("h2").size(),h3=d.select("h3").size();
        int alt=d.select("img:not([alt])").size();
        boolean noindex=(d.select("meta[name=robots],meta[name=googlebot]").eachAttr("content").toString()+" "+headerRobots).toLowerCase(Locale.ROOT).contains("noindex");
        boolean schema=!d.select("script[type=application/ld+json]").isEmpty();
        Set<String> links=new LinkedHashSet<>(); int external=0;
        for(var a:d.select("a[href]")){
            URI target=crawler.normalize(a.absUrl("href"),u);
            if(target!=null&&links.size()<500)links.add(target.toString());
            else if(a.absUrl("href").startsWith("http")&&target==null)external++;
        }
        String body=d.body()==null?"":d.body().text().strip();
        int words=body.isEmpty()?0:body.split("\\s+").length;
        if(title.isBlank())issues.add("MISSING_TITLE");else if(title.length()>65)issues.add("LONG_TITLE");
        if(desc.isBlank())issues.add("MISSING_DESCRIPTION");else if(desc.length()>160)issues.add("LONG_DESCRIPTION");
        if(h1!=1)issues.add("H1_COUNT");if(h3>0&&h2==0)issues.add("HEADING_GAP");
        if(alt>0)issues.add("MISSING_IMAGE_ALT");if(canonical.isBlank())issues.add("MISSING_CANONICAL");
        if(noindex)issues.add("NOINDEX");if(!schema)issues.add("NO_JSON_LD");if(words<100)issues.add("THIN_CONTENT_REVIEW");
        return new Page(u.toString(),status,title,desc,canonical,h1,h2,h3,words,alt,links.size(),external,schema,noindex,redirect,
          digest(body.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ")),issues,List.copyOf(links));
    }
    private Page failed(URI u,int code,String issue,String redirect){
        return new Page(u.toString(),code,"","","",0,0,0,0,0,0,0,false,false,redirect,"",new ArrayList<>(List.of(issue)),List.of());
    }
    public Report crawl(){
        Instant started=Instant.now(),deadline=started.plusSeconds(110);List<String> notes=new ArrayList<>();
        List<Page> results=new ArrayList<>(); Map<String,Boolean> bots=new LinkedHashMap<>();
        String robots;
        try{
            var r=fetch(origin.resolve("/robots.txt"));
            if(r.statusCode()==404||r.statusCode()==410){robots="";notes.add("robots.txt missing");}
            else if(r.statusCode()==200)robots=r.body();
            else throw new IllegalStateException();
        }catch(Exception e){return new Report(origin.toString(),started,0,List.of(),Map.of(),List.of("robots.txt unavailable; crawl stopped"),false);}
        for(String bot:List.of("Googlebot","Bingbot","OAI-SearchBot","Claude-SearchBot","PerplexityBot"))
            bots.put(bot,allowed(robots,bot,"/"));
        Queue<URI> queue=new ArrayDeque<>(),maps=new ArrayDeque<>();
        Set<URI> queued=new HashSet<>(),mapSeen=new HashSet<>();
        queue.add(origin.resolve("/"));queued.add(origin.resolve("/"));
        maps.add(origin.resolve("/sitemap.xml"));
        for(String line:robots.split("\\R")) if(line.toLowerCase(Locale.ROOT).startsWith("sitemap:")){
            URI u=normalize(line.substring(8).strip(),origin);if(u!=null)maps.add(u);
        }
        while(!maps.isEmpty()&&mapSeen.size()<10&&Instant.now().isBefore(deadline)){
            URI u=maps.remove();if(!mapSeen.add(u)||!allowed(robots,AGENT,u.getRawPath()))continue;
            try{
                var response=fetch(u);if(response.statusCode()!=200){notes.add("Sitemap unavailable: "+u);continue;}
                Document xml=Jsoup.parse(response.body(),u.toString(),Parser.xmlParser());
                for(var el:xml.select("sitemap > loc")){URI v=normalize(el.text(),u);if(v!=null&&maps.size()<20)maps.add(v);}
                for(var el:xml.select("url > loc")){URI v=normalize(el.text(),u);if(v!=null&&queued.size()<5000&&queued.add(v))queue.add(v);}
            }catch(Exception e){notes.add("Sitemap read failed: "+u);}
        }
        Map<String,List<Page>> titles=new HashMap<>(),hashes=new HashMap<>(); Map<String,Page> byUrl=new HashMap<>();
        boolean errors=false;
        while(!queue.isEmpty()&&results.size()<limit&&Instant.now().isBefore(deadline)){
            URI u=queue.remove();Page p;
            if(!allowed(robots,AGENT,u.getRawPath()))p=failed(u,0,"ROBOTS_BLOCKED","");
            else try{
                var r=fetch(u);int code=r.statusCode();String redirect=Optional.ofNullable(r.header("Location")).orElse("");
                if(code>=300&&code<400){
                    p=failed(u,code,"REDIRECT",redirect);URI v=normalize(redirect,u);if(v!=null&&queued.size()<5000&&queued.add(v))queue.add(v);
                }else if(code!=200)p=failed(u,code,"HTTP_"+code,"");
                else if(!Optional.ofNullable(r.contentType()).orElse("").toLowerCase(Locale.ROOT).contains("html"))p=failed(u,code,"NON_HTML","");
                else{
                    p=inspect(u,code,r.body(),Optional.ofNullable(r.header("X-Robots-Tag")).orElse(""),"",this);
                    if(!p.title().isBlank())titles.computeIfAbsent(p.title().toLowerCase(Locale.ROOT),k->new ArrayList<>()).add(p);
                    hashes.computeIfAbsent(p.fingerprint(),k->new ArrayList<>()).add(p);
                    for(String link:p.links()){URI v=URI.create(link);if(queued.size()<5000&&queued.add(v))queue.add(v);}
                }
            }catch(Exception e){p=failed(u,0,"FETCH_FAILED","");errors=true;}
            results.add(p);byUrl.put(p.url(),p);
        }
        for(var group:titles.values())if(group.size()>1)group.forEach(p->p.issues().add("DUPLICATE_TITLE"));
        for(var group:hashes.values())if(group.size()>1)group.forEach(p->p.issues().add("EXACT_DUPLICATE_TEXT"));
        for(Page p:results)for(String link:p.links()){
            Page target=byUrl.get(link);if(target!=null&&target.status()>=400)p.issues().add("BROKEN_LINK: "+link);
        }
        if(!queue.isEmpty())notes.add("Page/time limit reached; crawl is partial.");
        notes.add("HTML only; no JavaScript rendering, query URLs or external link crawling. Root robot rules are not proof of indexing. Configure network egress restrictions for crawler DNS rebinding defense.");
        return new Report(origin.toString(),started,results.size(),results,bots,notes,queue.isEmpty()&&maps.isEmpty()&&!errors&&queued.size()<5000);
    }
}
