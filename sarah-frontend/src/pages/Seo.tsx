import { useEffect, useState } from 'react';
import { api } from '../api';
import { Card, PageHeading, ErrorNotice } from '../components/Layout';
import { preview } from '../preview';
type History = {id:number;kind:string;subject:string;at:string};
type Page = {url:string;status:number;title:string;description:string;issues:string[]};
type Report = {results?:Page[];notes?:string[];complete?:boolean;answer?:string;citationUrls?:string[];brandMention?:boolean;ownDomainCited?:boolean;model?:string};
export function Seo(){
  const [target,setTarget]=useState('oreostudios.fr');
  const [url,setUrl]=useState('https://oreostudios.fr/');
  const [keyword,setKeyword]=useState('création site web Strasbourg');
  const [query,setQuery]=useState('Quelle agence web choisir à Strasbourg pour créer un site professionnel ?');
  const [provider,setProvider]=useState('openai');
  const [strategy,setStrategy]=useState('mobile');
  const [report,setReport]=useState<Report|null>(null);
  const [history,setHistory]=useState<History[]>([]);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState<string|null>(null);
  async function refresh(){if(!preview){const r=await api.get<History[]>('/api/v1/seo/history');setHistory(r.data);}}
  useEffect(()=>{void refresh().catch(()=>setError('Unable to load SEO history. Owner access required.'));},[]);
  async function run(kind:string){
    if(kind==='indexnow'&&!window.confirm('Confirm this URL was added, updated or deleted and should be submitted to IndexNow?'))return;
    setBusy(true);setError(null);
    try {const r=kind==='inspect'?await api.post<Report>('/api/v1/dashboard/search-console/inspect',{url}):await api.post<Report>('/api/v1/seo/'+kind,{target,url,keyword,query,provider,strategy,confirmSubmission:kind==='indexnow'},{timeout:130000});setReport(r.data);await refresh();}
    catch{setError('Report unavailable. Verify credentials, provider quota and owner access.');}
    finally{setBusy(false);}
  }
  async function load(id:number){setError(null);try{setReport((await api.get<Report>('/api/v1/seo/history/'+id)).data);}catch{setError('Could not load report.');}}
  return <><PageHeading eyebrow="Search intelligence" title="SEO & GEO" description="Audit your public website, inspect backlinks and measure AI citations. External reports may incur charges."/>
    <ErrorNotice message={error}/>
    <Card title="Website audit"><p>Discover same-origin HTML pages from links and sitemaps. Robots rules and crawl limits apply.</p>
      <button className="button" disabled={busy||preview} onClick={()=>void run('crawl')}>Run website audit</button>
      <p><label>Page URL <input value={url} onChange={e=>setUrl(e.target.value)}/></label> <select value={strategy} onChange={e=>setStrategy(e.target.value)}><option>mobile</option><option>desktop</option></select> <button className="button quiet" disabled={busy||preview} onClick={()=>void run('speed')}>PageSpeed</button></p>
      <button className="button quiet" disabled={busy||preview} onClick={()=>void run('inspect')}>Google index inspection</button> <button className="button quiet" disabled={busy||preview} onClick={()=>void run('bing')}>Bing traffic</button> <button className="button quiet" disabled={busy||preview} onClick={()=>void run('indexnow')}>Submit changed URL (IndexNow)</button>
    </Card>
    <Card title="Backlinks & keywords"><p><label>Domain <input value={target} onChange={e=>setTarget(e.target.value)}/></label></p>
      {['summary','backlinks','referring-domains','lost-links','backlink-trend','ranked'].map(k=><button key={k} className="button quiet" disabled={busy||preview} onClick={()=>void run(k)}>{k}</button>)}
      <p><label>Keyword <input value={keyword} onChange={e=>setKeyword(e.target.value)}/></label> <button className="button quiet" disabled={busy||preview} onClick={()=>void run('keywords')}>Keyword suggestions</button></p>
    </Card>
    <Card title="AI visibility"><p>Neutral prompts, native web search and returned citations. API results are samples, not consumer chatbot rankings.</p>
      <label>Query <textarea value={query} onChange={e=>setQuery(e.target.value)}/></label>
      <select value={provider} onChange={e=>setProvider(e.target.value)}><option value="openai">OpenAI</option><option value="anthropic">Claude</option><option value="gemini">Gemini</option></select>
      <button className="button" disabled={busy||preview} onClick={()=>void run('geo')}>Measure visibility</button>
    </Card>
    {busy&&<p role="status">Running report…</p>}
    {report&&<Card title="Report">{report.results?<><p>{report.results.length} pages · {report.complete?'Discovered queue completed':'Partial crawl'}</p>{report.notes?.map(n=><p key={n}>{n}</p>)}<div style={{overflowX:'auto'}}><table><thead><tr><th>URL</th><th>Status</th><th>Title</th><th>Issues</th></tr></thead><tbody>{report.results.map(p=><tr key={p.url}><td>{p.url}</td><td>{p.status}</td><td>{p.title}</td><td>{p.issues.join(', ')}</td></tr>)}</tbody></table></div></>:report.answer?<><p>{report.model} · Brand mention: {String(report.brandMention)} · Own-domain citation: {String(report.ownDomainCited)}</p><p style={{whiteSpace:'pre-wrap'}}>{report.answer}</p>{report.citationUrls?.map(u=><p key={u}>{u}</p>)}</>:<pre style={{overflow:'auto',maxHeight:480}}>{JSON.stringify(report,null,2)}</pre>}</Card>}
    <Card title="Report history">{history.map(h=><p key={h.id}><button className="button quiet small" disabled={busy} onClick={()=>void load(h.id)}>{h.kind} · {h.subject} · {new Date(h.at).toLocaleString()}</button></p>)}</Card>
  </>;
}
