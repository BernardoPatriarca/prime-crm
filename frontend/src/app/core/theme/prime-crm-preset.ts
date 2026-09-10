import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const PrimeCrmPreset = definePreset(Aura, {
  primitive: {
    borderRadius: {
      none: '0',
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '10px',
      xl: '14px',
    },
  },
  semantic: {
    transitionDuration: '0.16s',
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.color}',
      offset: '2px',
      shadow: '0 0 0 3px rgba(79, 70, 229, 0.18)',
    },
    primary: {
      50: '#EEF2FF',
      100: '#E0E7FF',
      200: '#C7D2FE',
      300: '#A5B4FC',
      400: '#818CF8',
      500: '#6366F1',
      600: '#4F46E5',
      700: '#4338CA',
      800: '#3730A3',
      900: '#312E81',
      950: '#1E1B4B',
    },
    formField: {
      borderRadius: '{border.radius.sm}',
      paddingX: '0.75rem',
      paddingY: '0.55rem',
      transitionDuration: '0.16s',
      focusBorderColor: '{primary.color}',
      sm: {
        paddingX: '0.6rem',
        paddingY: '0.4rem',
      },
    },
    content: {
      borderRadius: '{border.radius.lg}',
    },
    mask: {
      transitionDuration: '0.2s',
    },
    navigation: {
      item: {
        borderRadius: '{border.radius.sm}',
        transitionDuration: '0.16s',
      },
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}',
        },
        surface: {
          0: '#ffffff',
          50: '#F8F9FB',
          100: '#F1F3F7',
          200: '#E5E8EF',
          300: '#D3D8E3',
          400: '#A6AEC0',
          500: '#7B8598',
          600: '#5B6474',
          700: '#414957',
          800: '#2B313C',
          900: '#1A1F27',
          950: '#0E1116',
        },
        text: {
          color: '#1A1F27',
          hoverColor: '#0E1116',
          mutedColor: '#5B6474',
          hoverMutedColor: '#414957',
        },
        highlight: {
          background: 'rgba(79, 70, 229, 0.07)',
          focusBackground: 'rgba(79, 70, 229, 0.12)',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
        overlay: {
          select: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 10px 24px -12px rgba(14, 17, 22, 0.22), 0 2px 6px -3px rgba(14, 17, 22, 0.10)',
          },
          popover: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 10px 24px -12px rgba(14, 17, 22, 0.22), 0 2px 6px -3px rgba(14, 17, 22, 0.10)',
          },
          modal: {
            borderRadius: '{border.radius.xl}',
            shadow: '0 24px 56px -20px rgba(14, 17, 22, 0.30), 0 8px 20px -14px rgba(14, 17, 22, 0.16)',
          },
          navigation: {
            shadow: '0 10px 24px -12px rgba(14, 17, 22, 0.22)',
          },
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '#12141C',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        surface: {
          0: '#FFFFFF',
          50: '#F6F7FA',
          100: '#EBEDF3',
          200: '#D5D9E3',
          300: '#AEB5C4',
          400: '#858DA0',
          500: '#68707F',
          600: '#4A5261',
          700: '#333A47',
          800: '#212733',
          900: '#161B24',
          950: '#0E121A',
        },
        text: {
          color: '#EDEFF4',
          hoverColor: '#ffffff',
          mutedColor: '#9AA2B2',
          hoverMutedColor: '#C7CCD8',
        },
        highlight: {
          background: 'rgba(129, 140, 248, 0.16)',
          focusBackground: 'rgba(129, 140, 248, 0.24)',
          color: '#E0E7FF',
          focusColor: '#ffffff',
        },
        overlay: {
          select: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 10px 24px -12px rgba(0, 0, 0, 0.66), 0 2px 6px -3px rgba(0, 0, 0, 0.50)',
          },
          popover: {
            borderRadius: '{border.radius.lg}',
            shadow: '0 10px 24px -12px rgba(0, 0, 0, 0.66), 0 2px 6px -3px rgba(0, 0, 0, 0.50)',
          },
          modal: {
            borderRadius: '{border.radius.xl}',
            shadow: '0 24px 56px -20px rgba(0, 0, 0, 0.74), 0 8px 20px -14px rgba(0, 0, 0, 0.55)',
          },
          navigation: {
            shadow: '0 10px 24px -12px rgba(0, 0, 0, 0.66)',
          },
        },
      },
    },
  },
});
