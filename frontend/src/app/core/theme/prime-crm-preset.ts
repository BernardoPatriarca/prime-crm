import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const PrimeCrmPreset = definePreset(Aura, {
  primitive: {
    borderRadius: {
      none: '0',
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px',
    },
  },
  semantic: {
    transitionDuration: '0.2s',
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.color}',
      offset: '2px',
      shadow: '0 0 0 4px rgba(30, 94, 255, 0.16)',
    },
    primary: {
      50: '#EEF3FF',
      100: '#DCE7FF',
      200: '#BFD3FF',
      300: '#8FB4FA',
      400: '#3B82F6',
      500: '#1E5EFF',
      600: '#1A4FD1',
      700: '#153FA8',
      800: '#123480',
      900: '#0B3C91',
      950: '#071F4A',
    },
    formField: {
      borderRadius: '{border.radius.md}',
      paddingX: '0.85rem',
      paddingY: '0.6rem',
      transitionDuration: '0.2s',
      focusBorderColor: '{primary.color}',
    },
    content: {
      borderRadius: '{border.radius.lg}',
    },
    mask: {
      transitionDuration: '0.25s',
    },
    navigation: {
      item: {
        borderRadius: '{border.radius.md}',
        transitionDuration: '0.2s',
      },
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.600}',
          activeColor: '{primary.700}',
        },
        surface: {
          0: '#ffffff',
          50: '#F5F7FA',
          100: '#EBEEF3',
          200: '#DDE2EA',
          300: '#C6CEDA',
          400: '#9BA7BB',
          500: '#71809A',
          600: '#576078',
          700: '#3F4658',
          800: '#282D3A',
          900: '#1F2937',
          950: '#111827',
        },
        text: {
          color: '#1F2937',
          hoverColor: '#111827',
          mutedColor: '#6B7280',
          hoverMutedColor: '#4B5563',
        },
        highlight: {
          background: 'rgba(30, 94, 255, 0.08)',
          focusBackground: 'rgba(30, 94, 255, 0.14)',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
        overlay: {
          select: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 12px 28px -10px rgba(11, 60, 145, 0.20), 0 4px 10px -6px rgba(15, 23, 42, 0.10)',
          },
          popover: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 12px 28px -10px rgba(11, 60, 145, 0.20), 0 4px 10px -6px rgba(15, 23, 42, 0.10)',
          },
          modal: {
            borderRadius: '{border.radius.xl}',
            shadow: '0 28px 60px -18px rgba(11, 60, 145, 0.28), 0 10px 24px -12px rgba(15, 23, 42, 0.16)',
          },
          navigation: {
            shadow: '0 12px 28px -10px rgba(11, 60, 145, 0.20)',
          },
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '#0B1220',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        surface: {
          0: '#FFFFFF',
          50: '#F6F7FA',
          100: '#ECEEF3',
          200: '#D6DAE3',
          300: '#B2B9C8',
          400: '#8A93A6',
          500: '#6B7488',
          600: '#4D5568',
          700: '#373E50',
          800: '#242A39',
          900: '#171C28',
          950: '#0F131C',
        },
        text: {
          color: '#F3F4F6',
          hoverColor: '#ffffff',
          mutedColor: '#9CA3AF',
          hoverMutedColor: '#D1D5DB',
        },
        highlight: {
          background: 'rgba(59, 130, 246, 0.16)',
          focusBackground: 'rgba(59, 130, 246, 0.24)',
          color: '#DCE7FF',
          focusColor: '#ffffff',
        },
        overlay: {
          select: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 12px 28px -10px rgba(0, 0, 0, 0.60), 0 4px 10px -6px rgba(0, 0, 0, 0.45)',
          },
          popover: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 12px 28px -10px rgba(0, 0, 0, 0.60), 0 4px 10px -6px rgba(0, 0, 0, 0.45)',
          },
          modal: {
            borderRadius: '{border.radius.xl}',
            shadow: '0 28px 60px -18px rgba(0, 0, 0, 0.70), 0 10px 24px -12px rgba(0, 0, 0, 0.50)',
          },
          navigation: {
            shadow: '0 12px 28px -10px rgba(0, 0, 0, 0.60)',
          },
        },
      },
    },
  },
});
