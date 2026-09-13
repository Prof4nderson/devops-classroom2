import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'
import { AuthProvider } from './contexts/AuthContext'
import { UxProvider } from './contexts/UxContext'
import { Toaster } from 'react-hot-toast'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <UxProvider>
    <AuthProvider>
      <Toaster
        position="top-right"
        toastOptions={{
          className: 'ux-toast',
          style: {
            background: 'var(--toast-bg, #12121c)',
            color: 'var(--toast-fg, #f5f7ff)',
            border: '1px solid var(--glass-border-lit, rgba(255,255,255,0.14))',
            borderRadius: 'calc(var(--radius-card, 16px) * 0.8)',
          },
        }}
      />
      <App />
    </AuthProvider>
  </UxProvider>,
)
