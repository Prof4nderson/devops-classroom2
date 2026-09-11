import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ChatWebSocket } from '../services/websocket';
import { ChatMessage, Aula, Usuario } from '../types';
import {
  Send, Image, Code, Bot, Users, Clock, Trophy, LogOut, Reply, X,
  PlayCircle, StopCircle, BookOpen,
} from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../services/api';

interface ChatRoomProps {
  aula: Aula;
  user: Usuario;
  onLeave: () => void;
}

const ChatRoom: React.FC<ChatRoomProps> = ({ aula, user, onLeave }) => {
  const [isAiThinking, setIsAiThinking] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null);
  const [presentes, setPresentes] = useState<Record<number, string>>({});
  const [showCodeEditor, setShowCodeEditor] = useState(false);
  const [codeText, setCodeText] = useState('');
  const [codeLanguage, setCodeLanguage] = useState('bash');
  const [activeQuiz, setActiveQuiz] = useState<any>(null);
  const [quizResult, setQuizResult] = useState<any>(null);
  const [minhaResposta, setMinhaResposta] = useState<string | null>(null);
  const [feedbackQuiz, setFeedbackQuiz] = useState<{ correta: boolean; correcao?: string } | null>(null);
  const [quizSegundos, setQuizSegundos] = useState<number | null>(null);
  const [enviandoResposta, setEnviandoResposta] = useState(false);
  const [showQuizCreator, setShowQuizCreator] = useState(false);
  const [quizForm, setQuizForm] = useState({
    pergunta: '',
    opcoes: ['', '', '', ''],
    respostaCorreta: '',
    tempoLimite: 30,
  });
  const [aulaAtual, setAulaAtual] = useState<any>(aula);
  const [showFecharAula, setShowFecharAula] = useState(false);
  const [diarioForm, setDiarioForm] = useState({ conteudoMinistrado: '', observacoes: '' });
  const ehDocente = user.tipo === 'PROFESSOR' || user.tipo === 'ADMIN';

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const wsRef = useRef<ChatWebSocket | null>(null);

  // Helper para normalizar dados de presença em um Map/Record
  const processarPresentes = (dados: any): Record<number, string> => {
    const mapa: Record<number, string> = {};
    const lista = Array.isArray(dados) ? dados : dados?.presentes || dados;

    if (Array.isArray(lista)) {
      lista.forEach((u: any) => {
        if (u && typeof u === 'object') {
          const id = u.id || u.usuarioId;
          const nome = u.nome || u.usuarioNome || u.nomeUsuario || u.userName;
          if (id && nome) mapa[id] = nome;
        }
      });
    } else if (typeof lista === 'object' && lista !== null) {
      Object.entries(lista).forEach(([key, val]) => {
        if (typeof val === 'string') mapa[Number(key)] = val;
        else if (val && typeof val === 'object') {
          mapa[Number(key)] =
            (val as any).nome ||
            (val as any).usuarioNome ||
            (val as any).nomeUsuario ||
            `Usuário ${key}`;
        }
      });
    }
    return mapa;
  };

  // Contagem regressiva do quiz ativo
  useEffect(() => {
    if (quizSegundos === null || quizSegundos <= 0) return;
    const t = setInterval(() => {
      setQuizSegundos((v) => (v === null ? null : Math.max(0, v - 1)));
    }, 1000);
    return () => clearInterval(t);
  }, [quizSegundos]);

  // Função para desconectar e sair da sala
  const handleLeave = async () => {
    try {
      if (aula?.id && user?.id) {
        await api
          .delete(`/api/aulas/${aula.id}/presenca`, {
            params: { usuarioId: user.id },
          })
          .catch(() => {});
      }
    } finally {
      wsRef.current?.leave();
      toast.success('Você saiu da aula');
      onLeave();
    }
  };

  // WebSocket, Presença e Carregamento do Histórico
  useEffect(() => {
    if (user?.id && user?.nome) {
      setPresentes((prev) => ({ ...prev, [user.id]: user.nome }));
    }

    const handlePresence = (data: any) => {
      const mapaAtualizado = processarPresentes(data);
      setPresentes((prev) => ({
        ...prev,
        ...mapaAtualizado,
        ...(user?.id && user?.nome ? { [user.id]: user.nome } : {}),
      }));
    };

    const handleQuiz = (data: any) => {
      if (!data?.tipo) return;

      if (data.tipo === 'QUIZ_CREATED') {
        setActiveQuiz(data);
        setQuizResult(null);
        setMinhaResposta(null);
        setFeedbackQuiz(null);
        setQuizSegundos(data.tempoLimiteSegundos ?? null);
        setShowQuizCreator(false);
        toast('Novo quiz do professor!', { icon: '🎯' });
        return;
      }

      if (data.tipo === 'QUIZ_RESULT') {
        setQuizResult(data);
        setActiveQuiz((prev: any) => (prev && prev.quizId === data.quizId ? { ...prev, ...data } : prev));
        return;
      }

      if (data.tipo === 'QUIZ_CLOSED') {
        setQuizResult(data);
        setQuizSegundos(0);
        setActiveQuiz((prev: any) =>
          prev && prev.quizId === data.quizId ? { ...prev, ...data, status: 'FINALIZADO' } : prev
        );
        toast('Quiz encerrado', { icon: '🏁' });
      }
    };

    const ws = new ChatWebSocket(
      aula.id,
      (newMsg) =>
        setMessages((prev) => {
          if (
            prev.some(
              (m) =>
                m.id === newMsg.id ||
                (m.conteudo === newMsg.conteudo &&
                  m.usuarioId === newMsg.usuarioId &&
                  m.criadoEm === newMsg.criadoEm)
            )
          ) {
            return prev;
          }
          return [...prev, newMsg];
        }),
      handlePresence,
      handleQuiz
    );

    wsRef.current = ws;
    ws.connect();

    const inicializarSala = async () => {
      if (!aula?.id) return;

      try {
        const resMsgs = await api.get(`/api/aulas/${aula.id}/mensagens`);
        if (Array.isArray(resMsgs.data)) {
          setMessages(resMsgs.data);
        }
      } catch (err) {
        console.warn('Histórico REST indisponível.');
      }

      try {
        await api.post(`/api/aulas/${aula.id}/presenca`, { usuarioId: user.id });
      } catch (err) {
        await api.post(`/api/aulas/${aula.id}/presenca?usuarioId=${user.id}`).catch(() => {});
      }

      try {
        const resPresenca = await api.get(`/api/aulas/${aula.id}/presentes`);
        if (resPresenca?.data) {
          const mapaApi = processarPresentes(resPresenca.data);
          setPresentes((prev) => ({
            ...prev,
            ...mapaApi,
            [user.id]: user.nome,
          }));
        }
      } catch (err) {
        console.warn('Busca de presentes via REST falhou. Mantendo presença local.');
      }
    };

    inicializarSala();

    return () => {
      ws.leave();
    };
  }, [aula.id, user.id, user.nome]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = useCallback(async () => {
    if (!inputText.trim()) return;

    const textoEnviar = inputText;
    setInputText('');

    wsRef.current?.sendMessage(textoEnviar, 'TEXT', undefined, undefined, undefined, replyTo?.id ?? null);
    setReplyTo(null);

    if (textoEnviar.toLowerCase().includes('@coder')) {
      setIsAiThinking(true);

      try {
        const perguntaLimpa = textoEnviar.replace(/@coder/gi, '').trim();

        await api.post('/api/rag/chat', perguntaLimpa, {
          params: { sessionId: `aula-${aula.id}` },
          headers: { 'Content-Type': 'text/plain' },
        });
      } catch (error) {
        console.error('Erro ao consultar o agente @Coder:', error);
        toast.error('O agente @Coder não conseguiu responder no momento.');
      } finally {
        setIsAiThinking(false);
      }
    }
  }, [inputText, aula.id]);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const isImage = file.type.startsWith('image/');
    const isPdf = file.type === 'application/pdf' || file.name.endsWith('.pdf');

    try {
      if (isPdf && user.tipo !== 'PROFESSOR' && user.tipo !== 'ADMIN') {
        toast.error('Somente professores podem enviar conteúdos para a base RAG.');
        return;
      }
      if (isPdf) {
        const toastId = toast.loading('Indexando PDF na base do @Coder...');

        await api.post('/api/rag/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });

        toast.dismiss(toastId);
        toast.success('PDF indexado! Agora você pode fazer perguntas ao @Coder.');

        wsRef.current?.sendMessage(
          `📄 Documento adicionado à base de conhecimento: **${file.name}**`,
          'TEXT'
        );
      } else {
        const endpoint = isImage ? '/api/uploads/image' : '/api/uploads/file';
        const response = await api.post(endpoint, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });

        const data = response.data;
        const tipo = isImage ? (inputText.includes('```') ? 'CODE' : 'IMAGE') : 'FILE';

        wsRef.current?.sendMessage(
          inputText || file.name,
          tipo,
          data.url,
          data.nomeArquivo,
          data.mimeType
        );
      }
    } catch (error) {
      console.error('Erro no upload:', error);
      toast.error('Erro ao processar o arquivo.');
    }

    e.target.value = '';
  };

  const sendCode = () => {
    if (!codeText.trim()) return;
    const content = `\`\`\`${codeLanguage}\n${codeText}\n\`\`\``;
    wsRef.current?.sendMessage(content, 'CODE');
    setCodeText('');
    setShowCodeEditor(false);
  };

  const sendScreenshot = () => {
    if (fileInputRef.current) {
      fileInputRef.current.accept = 'image/*';
      fileInputRef.current.click();
    }
  };

  const handleOptionChange = (index: number, value: string) => {
    const novasOpcoes = [...quizForm.opcoes];
    novasOpcoes[index] = value;
    setQuizForm((prev) => ({
      ...prev,
      opcoes: novasOpcoes,
      respostaCorreta:
        prev.respostaCorreta === prev.opcoes[index] || !prev.respostaCorreta
          ? value
          : prev.respostaCorreta,
    }));
  };

  const createQuiz = async () => {
    const opcoesValidas = quizForm.opcoes.map((o) => o.trim()).filter(Boolean);
    if (!quizForm.pergunta.trim() || opcoesValidas.length < 2) {
      toast.error('Preencha a pergunta e pelo menos 2 opções.');
      return;
    }
    const correta = opcoesValidas.includes(quizForm.respostaCorreta.trim())
      ? quizForm.respostaCorreta.trim()
      : opcoesValidas[0];

    const payload = {
      aulaId: aula.id,
      pergunta: quizForm.pergunta.trim(),
      opcoes: opcoesValidas,
      respostaCorreta: correta,
      tempoLimiteSegundos: quizForm.tempoLimite,
    };

    try {
      const response = await api.post('/api/quizzes', payload);
      const criado = response.data;
      setActiveQuiz({ ...criado, tipo: 'QUIZ_CREATED' });
      setQuizResult(null);
      setMinhaResposta(null);
      setFeedbackQuiz(null);
      setQuizSegundos(criado?.tempoLimiteSegundos ?? quizForm.tempoLimite);
      toast.success('Quiz publicado para a turma!');
    } catch (error) {
      // fallback: o backend persiste ao receber pelo WebSocket
      wsRef.current?.createQuiz(payload.pergunta, payload.opcoes, 0);
      toast('Quiz enviado via WebSocket', { icon: '⚡' });
    }

    setShowQuizCreator(false);
    setQuizForm({ pergunta: '', opcoes: ['', '', '', ''], respostaCorreta: '', tempoLimite: 30 });
  };

  const responderQuiz = async (opcao: string) => {
    if (!activeQuiz?.quizId || minhaResposta || enviandoResposta) return;
    setEnviandoResposta(true);
    try {
      const { data } = await api.post('/api/quizzes/responder', {
        quizId: activeQuiz.quizId,
        respostaSelecionada: opcao,
      });
      setMinhaResposta(opcao);
      setFeedbackQuiz({ correta: !!data?.correta });
      if (data?.correta) toast.success('Resposta correta!');
      else toast.error('Resposta incorreta');
    } catch (error: any) {
      const msg = error?.response?.data?.erro || error?.response?.data?.message;
      toast.error(msg || 'Não foi possível registrar a resposta');
    } finally {
      setEnviandoResposta(false);
    }
  };

  const finalizarQuiz = async () => {
    if (!activeQuiz?.quizId) return;
    try {
      const { data } = await api.post(`/api/quizzes/${activeQuiz.quizId}/finalizar`);
      setQuizResult(data);
      setActiveQuiz((prev: any) => ({ ...prev, ...data, status: 'FINALIZADO' }));
      toast.success('Quiz finalizado');
    } catch {
      toast.error('Erro ao finalizar o quiz');
    }
  };

  const iniciarAula = async () => {
    try {
      const { data } = await api.post(`/api/diario/aula/${aula.id}/iniciar`);
      setAulaAtual((prev: any) => ({ ...prev, ...data }));
      toast.success('Aula iniciada — diário aberto');
    } catch {
      toast.error('Não foi possível iniciar a aula');
    }
  };

  const finalizarAula = async () => {
    try {
      const { data } = await api.post(`/api/diario/aula/${aula.id}/finalizar`, diarioForm);
      setAulaAtual((prev: any) => ({ ...prev, ...data }));
      setShowFecharAula(false);
      toast.success('Aula finalizada e presenças consolidadas');
    } catch {
      toast.error('Não foi possível finalizar a aula');
    }
  };

  return (
    <div className="flex h-screen">
      {/* Sidebar - Presentes */}
      <div className="w-64 glass-bar border-r flex flex-col">
        <div className="p-4 border-b divider">
          <h3 className="text-xs font-semibold txt-dim uppercase tracking-wider flex items-center gap-2">
            <Users className="w-4 h-4 neon"/>
            Presentes ({Object.keys(presentes).length})
          </h3>
        </div>
        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          {Object.entries(presentes).map(([id, nome]) => (
            <div
              key={id}
              className="flex items-center gap-2 px-3 py-2 rounded-xl border border-transparent hover:border-white/10 hover:bg-white/5 transition-colors"
            >
              <div className="dot-live animate-pulse" />
              <span className="text-sm txt-dim truncate">{nome}</span>
              {parseInt(id) === user.id && <span className="text-xs neon ml-auto">(você)</span>}
            </div>
          ))}
          {Object.keys(presentes).length === 0 && (
            <p className="text-sm txt-faint text-center py-4">Ninguém presente ainda</p>
          )}
        </div>
      </div>

      {/* Chat Principal */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <div className="p-4 border-b divider glass-bar flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold title-glow">{aula?.titulo || 'Sala de Aula'}</h2>
            <p className="text-sm txt-dim">
              {aula?.curso?.nome} {aula?.status ? `• ${aula.status}` : ''}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 badge-neon">
              <Clock className="w-3.5 h-3.5"/>
              <span>{aula?.duracao || '0h'}</span>
            </div>

            {ehDocente && aulaAtual?.status !== 'EM_ANDAMENTO' && aulaAtual?.status !== 'FINALIZADA' && (
              <button onClick={iniciarAula} className="btn-primary text-xs px-3 py-1.5" title="Iniciar aula">
                <PlayCircle className="w-4 h-4" />
                Iniciar aula
              </button>
            )}

            {ehDocente && aulaAtual?.status === 'EM_ANDAMENTO' && (
              <button
                onClick={() => setShowFecharAula(true)}
                className="btn-secondary text-xs px-3 py-1.5"
                title="Finalizar aula e preencher o diário"
              >
                <StopCircle className="w-4 h-4 neon-amber" />
                Finalizar aula
              </button>
            )}

            <button
              onClick={handleLeave}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 border border-red-500/20 transition-all text-xs font-medium cursor-pointer"
              title="Sair da aula"
            >
              <LogOut className="w-3.5 h-3.5"/>
              <span>Sair</span>
            </button>
          </div>
        </div>

        {/* Painel do quiz ativo */}
        {activeQuiz && (
          <div className="mx-4 mt-4 p-4 quiz-panel">
            <div className="flex items-center justify-between gap-3 mb-2">
              <div className="flex items-center gap-2">
                <Trophy className="w-5 h-5 neon-violet" />
                <span className="text-sm font-semibold neon-violet">
                  Quiz {activeQuiz.professorNome ? `• ${activeQuiz.professorNome}` : ''}
                </span>
                {activeQuiz.status === 'FINALIZADO' && <span className="badge-ok">encerrado</span>}
              </div>
              <div className="flex items-center gap-2">
                {quizSegundos !== null && activeQuiz.status !== 'FINALIZADO' && (
                  <span className={`badge-neon ${quizSegundos === 0 ? 'opacity-60' : ''}`}>
                    <Clock className="w-3.5 h-3.5" />
                    {quizSegundos}s
                  </span>
                )}
                <button
                  onClick={() => {
                    setActiveQuiz(null);
                    setQuizResult(null);
                    setFeedbackQuiz(null);
                    setMinhaResposta(null);
                  }}
                  className="icon-btn"
                  title="Fechar"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            <p className="mb-3 font-medium">{activeQuiz.pergunta}</p>

            <div className="space-y-2">
              {(activeQuiz.opcoes || []).map((op: string, i: number) => {
                const contagem = quizResult?.contagem?.[op] ?? 0;
                const total = quizResult?.totalRespostas ?? 0;
                const pct = total > 0 ? Math.round((contagem / total) * 100) : 0;
                const escolhida = minhaResposta === op;
                const gabarito = activeQuiz.respostaCorreta;
                const revelado = !!gabarito && (activeQuiz.status === 'FINALIZADO' || ehDocente);
                const classe = revelado && op === gabarito
                  ? 'quiz-option quiz-option-correct'
                  : escolhida && feedbackQuiz && !feedbackQuiz.correta
                    ? 'quiz-option quiz-option-wrong'
                    : escolhida && feedbackQuiz?.correta
                      ? 'quiz-option quiz-option-correct'
                      : 'quiz-option';

                return (
                  <div key={i} className="space-y-1">
                    <button
                      onClick={() => responderQuiz(op)}
                      disabled={
                        ehDocente ||
                        !!minhaResposta ||
                        enviandoResposta ||
                        activeQuiz.status === 'FINALIZADO' ||
                        quizSegundos === 0
                      }
                      className={classe}
                    >
                      <span className="flex items-center justify-between gap-3">
                        <span>{op}</span>
                        {(ehDocente || activeQuiz.status === 'FINALIZADO') && (
                          <span className="text-xs txt-dim">{contagem} ({pct}%)</span>
                        )}
                      </span>
                    </button>
                    {(ehDocente || activeQuiz.status === 'FINALIZADO') && (
                      <div className="meter">
                        <span style={{ width: `${pct}%` }} />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="flex items-center justify-between gap-3 mt-3">
              <p className="text-xs txt-dim">
                {quizResult
                  ? `${quizResult.totalRespostas} resposta(s) • ${quizResult.acertos} acerto(s) • ${quizResult.percentualAcerto}%`
                  : minhaResposta
                    ? 'Resposta registrada. Aguardando a turma...'
                    : ehDocente
                      ? 'Aguardando respostas da turma...'
                      : 'Escolha uma opção'}
              </p>
              {ehDocente && activeQuiz.status !== 'FINALIZADO' && (
                <button onClick={finalizarQuiz} className="btn-secondary text-xs px-3 py-1.5">
                  Encerrar quiz
                </button>
              )}
            </div>

            {feedbackQuiz && (
              <p className={`text-xs mt-2 ${feedbackQuiz.correta ? 'neon-lime' : 'text-red-400'}`}>
                {feedbackQuiz.correta ? 'Você acertou!' : 'Você errou essa.'}
              </p>
            )}
          </div>
        )}

        {/* Finalizar aula: diário */}
        {showFecharAula && (
          <div className="mx-4 mt-4 p-4 card">
            <h4 className="text-sm font-semibold neon-amber mb-3 flex items-center gap-2">
              <BookOpen className="w-4 h-4" />
              Encerrar aula e registrar no diário
            </h4>
            <textarea
              className="input-field mb-2"
              rows={3}
              placeholder="Conteúdo ministrado..."
              value={diarioForm.conteudoMinistrado}
              onChange={(e) => setDiarioForm({ ...diarioForm, conteudoMinistrado: e.target.value })}
            />
            <textarea
              className="input-field mb-3"
              rows={2}
              placeholder="Observações (opcional)"
              value={diarioForm.observacoes}
              onChange={(e) => setDiarioForm({ ...diarioForm, observacoes: e.target.value })}
            />
            <p className="text-xs txt-faint mb-3">
              Ao finalizar, quem não registrou presença é marcado como ausente automaticamente.
            </p>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowFecharAula(false)} className="btn-secondary text-sm">
                Cancelar
              </button>
              <button onClick={finalizarAula} className="btn-primary text-sm">
                Finalizar aula
              </button>
            </div>
          </div>
        )}

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map((msg, idx) => {
            // ID de quem enviou a mensagem (busca em todas as variações possíveis)
            const autorId =
              msg.usuarioId ??
              (msg as any).usuario?.id ??
              (msg as any).usuario_id ??
              (msg as any).userId;

            // ID do usuário atualmente logado na tela (Professor ou Aluno)
            const currentUserId =
              user?.id ??
              (user as any)?.usuarioId ??
              (user as any)?.idUsuario;

            // Nome do autor
            const autorNome =
              msg.usuarioNome ||
              (msg as any).nomeUsuario ||
              (msg as any).nome ||
              (msg as any).usuario?.nome ||
              (autorId ? presentes[autorId] : null) ||
              'Usuário';

            const autorTipo =
              msg.usuarioTipo ||
              (msg as any).tipoUsuario ||
              (msg as any).usuario?.tipo;

            // Comparação blindada contra diferenças de tipo ou chave
            const ehMinha =
              autorId !== undefined &&
              currentUserId !== undefined &&
              String(autorId) === String(currentUserId);

            return (
              <div
                key={msg.id || idx}
                className={`flex ${ehMinha ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[70%] rounded-2xl px-4 py-3 ${
                    autorTipo === 'AI'
                      ? 'bubble-ai'
                      : ehMinha
                      ? 'bubble-mine'
                      : 'bubble-other'
                  }`}
                >
                  {!ehMinha && (
                    <p
                      className={`text-xs font-medium mb-1 ${
                        autorTipo === 'AI' ? 'neon-violet' : 'neon'
                      }`}
                    >
                      {autorTipo === 'AI' ? <Bot className="w-3 h-3 inline mr-1"/> : null}
                      {autorNome}
                    </p>
                  )}

                  {msg.replyToId && (() => {
                    const origem = messages.find((item) => item.id === msg.replyToId);
                    const nomeOrigem = msg.replyToNome || origem?.usuarioNome;
                    const textoOrigem = msg.replyToConteudo ?? origem?.conteudo;
                    if (!nomeOrigem && !textoOrigem) return null;
                    return (
                      <div className="mb-2 rounded-lg border-l-2 border-cyan-300/60 bg-black/20 px-2 py-1 text-xs text-cyan-100/80">
                        <span className="font-semibold">Respondendo a {nomeOrigem || 'usuário'}:</span>{' '}
                        {(textoOrigem || '').slice(0, 120)}
                      </div>
                    );
                  })()}
                  <p className="text-sm whitespace-pre-wrap">{msg.conteudo}</p>
                  <button
                    type="button"
                    onClick={() => setReplyTo(msg)}
                    className="mt-2 flex items-center gap-1 text-[11px] opacity-70 transition-opacity hover:opacity-100"
                    title="Responder a esta mensagem"
                  >
                    <Reply className="h-3 w-3" /> Responder
                  </button>

                  <p className={`text-xs mt-1 ${ehMinha ? 'opacity-70' : 'txt-faint'}`}>
                    {msg.criadoEm
                      ? new Date(msg.criadoEm).toLocaleTimeString('pt-BR', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      : ''}
                  </p>
                </div>
              </div>
            );
          })}

          {/* Indicador de Digitação da IA */}
          {isAiThinking && (
            <div className="flex justify-start">
              <div className="bubble-ai max-w-[70%] rounded-2xl px-4 py-3 flex items-center gap-2">
                <Bot className="w-4 h-4 neon-violet animate-spin"/>
                <span className="text-xs text-purple-300">
                  @Coder está pensando...já já vai digitar
                </span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div className="p-4 border-t divider glass-bar">
          {replyTo && (
            <div className="mb-3 flex items-start justify-between rounded-xl border border-cyan-300/20 bg-cyan-300/5 px-3 py-2 text-xs">
              <div className="min-w-0">
                <p className="font-semibold neon">Respondendo a {replyTo.usuarioNome || 'usuário'}</p>
                <p className="truncate txt-dim">{replyTo.conteudo}</p>
              </div>
              <button type="button" onClick={() => setReplyTo(null)} className="ml-2 opacity-70 hover:opacity-100" title="Cancelar resposta">
                <X className="h-4 w-4" />
              </button>
            </div>
          )}
          {showCodeEditor && (
            <div className="mb-3 p-3 glass rounded-2xl">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm txt-dim">Snippet de Código</span>
                <select
                  value={codeLanguage}
                  onChange={(e) => setCodeLanguage(e.target.value)}
                  className="input-field w-auto text-xs py-1"
                >
                  <option value="bash">Bash</option>
                  <option value="docker">Dockerfile</option>
                  <option value="yaml">YAML</option>
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                  <option value="javascript">JavaScript</option>
                  <option value="terraform">Terraform</option>
                  <option value="text">Texto</option>
                </select>
              </div>
              <textarea
                className="code-area"
                value={codeText}
                onChange={(e) => setCodeText(e.target.value)}
                placeholder="Cole ou digite seu código..."
              />
              <div className="flex justify-end gap-2 mt-2">
                <button
                  onClick={() => setShowCodeEditor(false)}
                  className="btn-secondary text-sm"
                >
                  Cancelar
                </button>
                <button onClick={sendCode} className="btn-primary text-sm">
                  Enviar Código
                </button>
              </div>
            </div>
          )}

          <div className="flex items-center gap-2">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileUpload}
              className="hidden"
              accept="image/*,.pdf,.zip,.tar.gz"
            />

            <button
              onClick={() => setShowCodeEditor(!showCodeEditor)}
              className={`icon-btn ${showCodeEditor ? 'icon-btn-on' : ''}`}
              title="Enviar código"
            >
              <Code className="w-5 h-5"/>
            </button>

            <button onClick={sendScreenshot} className="icon-btn" title="Compartilhar imagem">
              <Image className="w-5 h-5"/>
            </button>

            {user.tipo === 'PROFESSOR' && (
              <button
                onClick={() => setShowQuizCreator((v) => !v)}
                className={`icon-btn ${showQuizCreator ? 'icon-btn-on' : ''}`}
                title="Criar quiz"
              >
                <Trophy className="w-5 h-5 neon-violet"/>
              </button>
            )}

            <input
              type="text"
              className="input-field flex-1"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
              placeholder="Digite @Coder para perguntar à IA... (Enter para enviar)"
            />

            <button onClick={sendMessage} className="btn-primary p-2.5">
              <Send className="w-4 h-4"/>
            </button>
          </div>

          {/* Criação de quiz (professor) */}
          {showQuizCreator && ehDocente && (
            <div className="mt-3 p-4 quiz-panel">
              <h4 className="text-sm font-semibold neon-violet mb-3">Criar quiz</h4>
              <input
                className="input-field mb-2"
                placeholder="Pergunta..."
                value={quizForm.pergunta}
                onChange={(e) => setQuizForm({ ...quizForm, pergunta: e.target.value })}
              />
              {quizForm.opcoes.map((op, i) => (
                <div key={i} className="flex items-center gap-2 mb-2">
                  <input
                    type="radio"
                    name="respostaCorreta"
                    className="accent-current"
                    checked={!!op.trim() && quizForm.respostaCorreta === op}
                    onChange={() => setQuizForm({ ...quizForm, respostaCorreta: op })}
                    title="Marcar como resposta correta"
                  />
                  <input
                    type="text"
                    className="input-field"
                    placeholder={`Opção ${i + 1}`}
                    value={op}
                    onChange={(e) => handleOptionChange(i, e.target.value)}
                  />
                </div>
              ))}
              <p className="text-xs txt-faint mb-3">Marque o círculo da alternativa correta.</p>
              <div className="flex items-center gap-2 mb-3">
                <label className="text-sm txt-dim">Tempo (seg):</label>
                <input
                  type="number"
                  className="input-field w-24 py-1"
                  value={quizForm.tempoLimite}
                  onChange={(e) =>
                    setQuizForm({ ...quizForm, tempoLimite: parseInt(e.target.value) || 30 })
                  }
                />
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setShowQuizCreator(false)} className="btn-secondary text-sm">
                  Cancelar
                </button>
                <button onClick={createQuiz} className="btn-primary text-sm">
                  Publicar quiz
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ChatRoom;