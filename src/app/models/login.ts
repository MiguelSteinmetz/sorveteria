export type Cargo = 'GERENTE' | 'FUNCIONARIO';

export interface LoginRequest {
  login: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEm: string;
  nome: string;
  cargo: Cargo;
}

export interface CadastroRequest {
  nome: string;
  login: string;
  senha: string;
  cep: string;
}

export interface UsuarioResponse {
  id: number;
  nome: string;
  login: string;
  cargo: Cargo;
  ativo: boolean;
  cep: string;
}

export interface UsuarioRequest {
  nome: string;
  login: string;
  senha: string;
  cargo: Cargo;
  cep: string;
}

export interface UsuarioAtualizacaoRequest {
  nome: string;
  cargo: Cargo;
}
