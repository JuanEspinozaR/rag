"use client";

import { usePathname } from "next/navigation";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";

const pageTitles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/dashboard/knowledge": "Knowledge Sources",
  "/dashboard/chat": "Chat",
};

function getTitle(pathname: string): string {
  if (pageTitles[pathname]) return pageTitles[pathname];
  if (pathname.startsWith("/dashboard/chat/")) return "Conversation";
  return "RAG Platform";
}

export function Header() {
  const pathname = usePathname();
  const { theme, setTheme } = useTheme();
  const title = getTitle(pathname);

  return (
    <header className="flex h-14 items-center gap-4 border-b bg-background/95 px-6 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <h1 className="text-sm font-semibold flex-1">{title}</h1>

      <button
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        className="flex h-8 w-8 items-center justify-center rounded-md hover:bg-accent transition-colors"
        aria-label="Toggle theme"
      >
        <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
        <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
      </button>
    </header>
  );
}
