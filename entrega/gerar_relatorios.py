#!/usr/bin/env python3
"""
Gera os dois relatórios em PDF (Stakeholders e Likert) a partir da planilha preenchida.
Uso:  pip install openpyxl matplotlib reportlab
      python gerar_relatorios.py Planilha_Tabulacao_Testes.xlsx
Só usa as respostas digitadas nas células amarelas; nada é inventado. Quem calcula tudo é este script (não depende de a planilha ter sido recalculada).
"""
import sys, os, statistics as st, tempfile
import openpyxl
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

VERDE="#14503F"; AMAR="#F4B23E"; AZ="#3B7DD8"; CINZA="#CBD3CE"; VERM="#B3372A"
FAIXAS=["15 a 26","27 a 39","40 a 52","53 a 64","65 ou mais"]
TAR=["T1 Criar conta e entrar","T2 Cadastrar produto","T3 Criar pedido (2 itens)","T4 Mudar status e enviar WhatsApp","T5 Ver faturamento e status no Início"]
SUS=["Usaria com frequência","Mais complexo que o necessário","Fácil de usar","Precisaria de ajuda técnica","Funções bem integradas","Inconsistência demais","Aprenderia rápido","Muito confuso","Senti confiança","Precisei aprender muito antes"]
LIK=[("Entendi para que serve o app",1),("Telas fáceis de entender",1),("Difícil encontrar funções",-1),("Visual agradável",1),("Textos e botões legíveis",1),("Gráficos do Início ajudam",1),("Aviso por WhatsApp é útil",1),("Receio de errar/perder dados",-1),("Mensagens de erro ajudam",1),("Usaria / recomendaria",1),("App me cansou ou irritou",-1),("Satisfeito em geral",1)]

def faixa(i):
    return "15 a 26" if 15<=i<27 else "27 a 39" if i<40 else "40 a 52" if i<53 else "53 a 64" if i<65 else "65 ou mais" if i>=65 else None
def num(v):
    try: return float(v)
    except (TypeError, ValueError): return None
def sentimento(m): return "Positivo" if m>=4 else "Neutro" if m>=3 else "Negativo"
def sus_class(s): return "Excelente" if s>=80.3 else "Boa" if s>=68 else "Regular" if s>=51 else "Ruim"

ss=getSampleStyleSheet()
H1=ParagraphStyle("h1",parent=ss["Title"],textColor=colors.HexColor(VERDE),fontSize=20,alignment=0)
H2=ParagraphStyle("h2",parent=ss["Heading2"],textColor=colors.HexColor(VERDE),spaceBefore=12,keepWithNext=1)
B=ParagraphStyle("b",parent=ss["BodyText"],fontSize=10,leading=14)
S=ParagraphStyle("s",parent=B,fontSize=8.5,textColor=colors.HexColor("#5B6761"))
C=ParagraphStyle("c",parent=B,fontSize=8.5,leading=10.5)
def P(t,s=B): return Paragraph(t,s)
def tab(rows,widths=None,head=True):
    rows=[[P(str(c),C) if not isinstance(c,Paragraph) else c for c in r] for r in rows]
    t=Table(rows,colWidths=widths,repeatRows=1 if head else 0)
    sty=[("GRID",(0,0),(-1,-1),0.4,colors.HexColor(CINZA)),("VALIGN",(0,0),(-1,-1),"MIDDLE"),("TOPPADDING",(0,0),(-1,-1),3),("BOTTOMPADDING",(0,0),(-1,-1),3)]
    if head: sty+=[("BACKGROUND",(0,0),(-1,0),colors.HexColor(VERDE))]
    t.setStyle(TableStyle(sty))
    if head:
        for j in range(len(rows[0])): rows[0][j]=Paragraph("<font color='white'><b>%s</b></font>"%rows[0][j].text,C)
        t=Table(rows,colWidths=widths,repeatRows=1); t.setStyle(TableStyle(sty))
    return t
def capa(titulo,sub):
    return [P(titulo,H1),P(sub,B),P("InteriShopping · Equipe E · Gabriela de Alencar Marin e Nathalia Dourado Luz<br/>Sistemas para Dispositivos Móveis (PI5) · Prof. Me. Ozias G. Santos",S),Spacer(1,8)]
def rodape(c,d):
    c.saveState(); c.setFont("Helvetica",8); c.setFillColor(colors.HexColor("#5B6761"))
    c.drawString(2*cm,1.2*cm,"InteriShopping · Equipe E"); c.drawRightString(A4[0]-2*cm,1.2*cm,"Página %d"%d.page); c.restoreState()
def build(path,story):
    SimpleDocTemplate(path,pagesize=A4,leftMargin=2*cm,rightMargin=2*cm,topMargin=2*cm,bottomMargin=2*cm).build(story,onFirstPage=rodape,onLaterPages=rodape)

def stakeholders(wb,out,tmp):
    ts,sus=wb["STK_Tarefas"],wb["STK_SUS"]
    alunos=[]
    for r in range(2,5):
        nome=ts.cell(r,1).value
        tarefas=[(ts.cell(r,3+3*k).value, num(ts.cell(r,4+3*k).value), num(ts.cell(r,5+3*k).value)) for k in range(5)]
        q=[num(sus.cell(r,c).value) for c in range(2,12)]
        score=None
        if all(v is not None for v in q): score=(sum(q[i]-1 for i in range(0,10,2))+sum(5-q[i] for i in range(1,10,2)))*2.5
        alunos.append(dict(nome=nome,sob=ts.cell(r,2).value or "",tarefas=tarefas,q=q,sus=score,gostou=sus.cell(r,14).value,melhorar=sus.cell(r,15).value))
    comdados=[a for a in alunos if any(t[0] for t in a["tarefas"]) or a["sus"] is not None]
    story=capa("Relatório do questionário avaliativo dos stakeholders","Teste de usabilidade com 3 alunas de outras equipes")
    story+= [P("1. Objetivo",H2),P("Verificar se pessoas que não participaram do desenvolvimento conseguem executar as tarefas principais do InteriShopping e medir a usabilidade percebida com a escala SUS (System Usability Scale)."),
        P("2. Método",H2),
        P("Participaram os alunos <b>%s</b>, de outras equipes da turma. O teste foi feito <b>à distância</b>: cada aluna recebeu 5 tarefas, sem treinamento prévio, e relatou se concluiu (sim, com ajuda ou não); tempo e número de erros foram registrados apenas quando informados. Ao final respondeu ao SUS (10 afirmações, escala 1 a 5) e a duas perguntas abertas. A pontuação SUS vai de 0 a 100 e a referência de mercado é 68."%", ".join(a["nome"] for a in alunos))]
    if not comdados:
        story+=[P("<b>Dados ainda não coletados.</b> Preencha a planilha e gere o relatório novamente.")]; build(out,story); return "SEM DADOS"
    story.append(P("3. Resultados por tarefa",H2))
    rows=[["Tarefa"]+[a["nome"] for a in alunos]+["Concl.","Tempo médio","Erros médios"]]
    conc=[];tmeds=[]
    for k,t in enumerate(TAR):
        cel=[];ok=0;tem=[];er=[];n=0
        for a in alunos:
            c,tp,e=a["tarefas"][k]
            if c: n+=1
            cel.append(" · ".join([c or "—"]+(["%g s"%tp] if tp is not None else [])+(["%g err."%e] if e is not None else [])))
            if c=="Sim": ok+=1
            if tp is not None: tem.append(tp)
            if e is not None: er.append(e)
        conc.append((sum(1 for a in alunos if a["tarefas"][k][0]=="Sim"),sum(1 for a in alunos if a["tarefas"][k][0]=="Com ajuda"),sum(1 for a in alunos if a["tarefas"][k][0]=="Não")))
        tmeds.append(st.mean(tem) if tem else 0)
        rows.append([t]+cel+["%d de %d"%(ok,n) if n else "—","%.0f s"%st.mean(tem) if tem else "—","%.1f"%st.mean(er) if er else "—"])
    story.append(tab(rows,[3.6*cm]+[2.8*cm]*3+[1.8*cm,1.6*cm,1.6*cm]))
    fig,ax=plt.subplots(figsize=(7,3)); y=range(len(TAR))
    sim=[c[0] for c in conc]; aj=[c[1] for c in conc]; nao=[c[2] for c in conc]
    ax.barh(y,sim,color=VERDE,label="Sim"); ax.barh(y,aj,left=sim,color=AMAR,label="Com ajuda"); ax.barh(y,nao,left=[a+b for a,b in zip(sim,aj)],color=VERM,label="Não")
    ax.set_yticks(list(y)); ax.set_yticklabels(TAR,fontsize=8); ax.invert_yaxis(); ax.set_xlabel("Nº de alunos"); ax.legend(fontsize=8,ncol=3,loc="lower right"); ax.set_title("Conclusão das tarefas",fontsize=10)
    fig.tight_layout(); p1=os.path.join(tmp,"stk1.png"); fig.savefig(p1,dpi=160); plt.close(fig)
    story+=[Spacer(1,6),Image(p1,16*cm,6.9*cm)]
    story.append(P("4. Escala SUS",H2))
    sc=[a for a in alunos if a["sus"] is not None]
    if sc:
        story.append(tab([["Aluno","Pontuação SUS","Classificação"]]+[[a["nome"],"%.1f"%a["sus"],sus_class(a["sus"])] for a in sc],[5*cm,4*cm,4*cm]))
        media=st.mean(a["sus"] for a in sc)
        story.append(P("Média SUS: <b>%.1f</b> (%s), com %d respondente(s). Referência de mercado: 68."%(media,sus_class(media),len(sc))))
        medq=[st.mean(a["q"][i] for a in sc) for i in range(10)]
        # nota ajustada: 0-4 (quanto maior melhor)
        adj=[(m-1) if i%2==0 else (5-m) for i,m in enumerate(medq)]
        fig,ax=plt.subplots(figsize=(7,3.2)); ax.bar(range(1,11),adj,color=[VERDE if v>=3 else AMAR if v>=2 else VERM for v in adj]); ax.set_ylim(0,4); ax.set_xticks(range(1,11)); ax.set_xlabel("Afirmação do SUS (Q1 a Q10)"); ax.set_ylabel("Contribuição média (0 a 4)"); ax.set_title("Contribuição de cada afirmação para a nota SUS",fontsize=10)
        fig.tight_layout(); p2=os.path.join(tmp,"stk2.png"); fig.savefig(p2,dpi=160); plt.close(fig)
        story+=[Spacer(1,6),Image(p2,16*cm,7.3*cm)]
        pior=sorted(range(10),key=lambda i:adj[i])[:2]
    else: pior=[]
    story.append(P("5. Comentários dos alunos (transcritos)",H2))
    ha=False
    for a in alunos:
        if a["gostou"] or a["melhorar"]:
            ha=True; story.append(P("<b>%s</b> · Gostou: %s · Melhorar: %s"%(a["nome"],a["gostou"] or "não informado",a["melhorar"] or "não informado")))
    if not ha: story.append(P("As alunas não registraram comentários nas perguntas abertas."))
    story.append(P("6. Análise",H2))
    lin=[]
    falhas=[TAR[k] for k,c in enumerate(conc) if c[1]+c[2]>0]
    lin.append("Todas as tarefas foram concluídas sem ajuda por todos os alunos que as tentaram." if not falhas and sum(conc[0])>0 else ("Tarefas em que houve ajuda ou desistência: %s."%"; ".join(falhas) if falhas else "Há tarefas ainda sem registro."))
    if tmeds and any(tmeds): lin.append("A tarefa mais demorada foi <b>%s</b> (média de %.0f s)."%(TAR[max(range(5),key=lambda i:tmeds[i])],max(tmeds)))
    if pior: lin.append("As afirmações do SUS com menor contribuição foram Q%d (%s) e Q%d (%s); foram as menos pontuadas; mesmo assim, só indicam onde há mais espaço para melhorar."%(pior[0]+1,SUS[pior[0]],pior[1]+1,SUS[pior[1]]))
    for l in lin: story.append(P("• "+l))
    story+=[P("7. Limitações",H2),P("Amostra de apenas 3 alunas, todas da mesma turma, o que não permite generalizar os resultados. O teste foi à distância, sem observação direta: a conclusão das tarefas é relatada pelas próprias participantes e não há medição de tempo nem de erros. Os números são indicativos e servem para orientar correções, não para comparar o app com outros.")]
    build(out,story); return "ok"

def likert(wb,out,tmp):
    ws=wb["LIKERT_Publico"]; pes=[]
    for r in range(2,32):
        idade=num(ws.cell(r,2).value); resp=[num(ws.cell(r,c).value) for c in range(4,16)]
        if idade is None or any(v is None for v in resp): continue
        adj=[(6-v) if LIK[i][1]<0 else v for i,v in enumerate(resp)]
        m=sum(adj)/12
        pes.append(dict(cod=ws.cell(r,1).value,idade=int(idade),f=faixa(idade),adj=adj,m=m,s=sentimento(m),com=ws.cell(r,18).value))
    story=capa("Relatório do questionário Likert: análise de sentimento do público geral","Teste com pessoas comuns, por faixa etária")
    pes=[p for p in pes if p["f"]]
    story+=[P("1. Objetivo",H2),P("Medir a percepção de pessoas comuns sobre o InteriShopping (clareza, visual, utilidade, confiança e satisfação) e classificar o sentimento em positivo, neutro ou negativo, comparando cinco faixas etárias."),
        P("2. Método",H2),P("Questionário com 12 afirmações em escala Likert de 1 (discordo totalmente) a 5 (concordo totalmente). Três afirmações são negativas (L3, L8 e L11) e foram invertidas (6 menos a resposta) antes do cálculo. A média das 12 respostas de cada pessoa define o sentimento: <b>4,0 ou mais = positivo</b>; <b>3,0 a 3,99 = neutro</b>; <b>abaixo de 3,0 = negativo</b>. Os participantes estão identificados só por código (P01, P02...).")]
    if not pes:
        story.append(P("<b>Dados ainda não coletados.</b> Preencha a planilha e gere o relatório novamente.")); build(out,story); return "SEM DADOS"
    cont={f:[p for p in pes if p["f"]==f] for f in FAIXAS}
    aviso=[]
    if len(pes)<10: aviso.append("total de %d pessoas (mínimo 10)"%len(pes))
    for f in FAIXAS:
        if len(cont[f])<2: aviso.append("faixa %s com %d pessoa(s)"%(f,len(cont[f])))
    story.append(P("3. Amostra",H2))
    story.append(tab([["Faixa etária","Pessoas"]]+[[f,str(len(cont[f]))] for f in FAIXAS]+[["Total",str(len(pes))]],[5*cm,3*cm]))
    if aviso: story.append(P("<font color='#B3372A'><b>Atenção:</b> cobertura abaixo do exigido: %s.</font>"%"; ".join(aviso)))
    story.append(P("4. Resultados",H2))
    mg=st.mean(p["m"] for p in pes); n=len(pes)
    cs={s:sum(1 for p in pes if p["s"]==s) for s in("Positivo","Neutro","Negativo")}
    story.append(P("Média geral: <b>%.2f</b> de 5. Positivo: %d (%.0f%%) · Neutro: %d (%.0f%%) · Negativo: %d (%.0f%%)."%(mg,cs["Positivo"],100*cs["Positivo"]/n,cs["Neutro"],100*cs["Neutro"]/n,cs["Negativo"],100*cs["Negativo"]/n)))
    fig,ax=plt.subplots(figsize=(7,3)); fs=[f for f in FAIXAS if cont[f]]
    ax.bar(fs,[st.mean(p["m"] for p in cont[f]) for f in fs],color=VERDE); ax.axhline(4,color=AMAR,ls="--",lw=1); ax.axhline(3,color=VERM,ls="--",lw=1); ax.set_ylim(1,5); ax.set_ylabel("Média (1 a 5)"); ax.set_title("Sentimento médio por faixa etária (linhas: 4,0 positivo; 3,0 neutro)",fontsize=9)
    fig.tight_layout(); p1=os.path.join(tmp,"lk1.png"); fig.savefig(p1,dpi=160); plt.close(fig)
    fig,ax=plt.subplots(figsize=(7,3)); left=[0]*len(fs)
    for s,cor in(("Positivo",VERDE),("Neutro",AMAR),("Negativo",VERM)):
        v=[sum(1 for p in cont[f] if p["s"]==s) for f in fs]; ax.bar(fs,v,bottom=left,color=cor,label=s); left=[a+b for a,b in zip(left,v)]
    ax.set_ylabel("Pessoas"); ax.legend(fontsize=8); ax.set_title("Distribuição do sentimento por faixa etária",fontsize=10)
    fig.tight_layout(); p2=os.path.join(tmp,"lk2.png"); fig.savefig(p2,dpi=160); plt.close(fig)
    mq=[st.mean(p["adj"][i] for p in pes) for i in range(12)]
    fig,ax=plt.subplots(figsize=(7,4)); ax.barh(range(12),mq,color=[VERDE if v>=4 else AMAR if v>=3 else VERM for v in mq]); ax.set_yticks(range(12)); ax.set_yticklabels(["L%d %s"%(i+1,LIK[i][0]) for i in range(12)],fontsize=7.5); ax.invert_yaxis(); ax.set_xlim(1,5); ax.set_xlabel("Média (1 a 5, negativas já invertidas)"); ax.set_title("Média por afirmação",fontsize=10)
    fig.tight_layout(); p3=os.path.join(tmp,"lk3.png"); fig.savefig(p3,dpi=160); plt.close(fig)
    story+=[Image(p1,15*cm,6.4*cm),Spacer(1,4),Image(p2,15*cm,6.4*cm),PageBreak(),Image(p3,14*cm,8*cm)]
    story.append(P("Resultados por faixa etária",H2))
    story.append(tab([["Faixa","Pessoas","Média","Positivo","Neutro","Negativo"]]+[[f,len(cont[f]),"%.2f"%st.mean(p["m"] for p in cont[f]) if cont[f] else "—"]+[sum(1 for p in cont[f] if p["s"]==s) for s in("Positivo","Neutro","Negativo")] for f in FAIXAS],[3.5*cm,2*cm,2*cm,2.5*cm,2.5*cm,2.5*cm]))
    story.append(P("5. Comentários (transcritos, sem identificação)",H2))
    com=[p for p in pes if p["com"]]
    for p in com: story.append(P("<b>%s (%s):</b> %s"%(p["cod"],p["f"],p["com"])))
    if not com: story.append(P("Nenhum comentário registrado."))
    story.append(P("6. Análise",H2))
    lin=[]
    ordem=sorted(range(12),key=lambda i:mq[i])
    lin.append("Afirmação mais bem avaliada: L%d (%s), %.2f. Menos bem avaliada: L%d (%s), %.2f."%(ordem[-1]+1,LIK[ordem[-1]][0],mq[ordem[-1]],ordem[0]+1,LIK[ordem[0]][0],mq[ordem[0]]))
    mf={f:st.mean(p["m"] for p in cont[f]) for f in fs}
    if len(mf)>1: lin.append("A faixa com maior média foi %s (%.2f) e a com menor foi %s (%.2f)."%(max(mf,key=mf.get),max(mf.values()),min(mf,key=mf.get),min(mf.values())))
    lin.append("Sentimento geral: %s (média %.2f)."%(sentimento(mg).lower(),mg))
    for l in lin: story.append(P("• "+l))
    story+=[P("7. Limitações",H2),P("Amostra pequena e por conveniência (pessoas próximas), com poucas pessoas por faixa etária; as diferenças entre faixas são indicativas e não têm significância estatística. A análise de sentimento é feita por regra simples sobre a escala Likert, sem análise de texto dos comentários.")]
    build(out,story); return "ok"

if __name__=="__main__":
    arq=sys.argv[1] if len(sys.argv)>1 else "Planilha_Tabulacao_Testes.xlsx"
    wb=openpyxl.load_workbook(arq,data_only=False)
    pasta=os.path.dirname(os.path.abspath(arq)); tmp=tempfile.mkdtemp()
    print("Stakeholders:",stakeholders(wb,os.path.join(pasta,"Relatorio_Stakeholders.pdf"),tmp))
    print("Likert:",likert(wb,os.path.join(pasta,"Relatorio_Likert_Publico.pdf"),tmp))
