import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the navigation app', () => {
  render(<App />);
  expect(screen.getByText(/islamabad road navigation/i)).toBeInTheDocument();
});
