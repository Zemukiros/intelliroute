import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "IntelliRoute — Route Intelligence Platform",
  description:
    "AI-assisted route intelligence: shortest-path calculation over a weighted road network with measured performance.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
