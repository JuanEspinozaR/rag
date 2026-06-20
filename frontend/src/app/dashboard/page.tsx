import { Database, MessageSquare, BookOpen, Activity } from "lucide-react";
import Link from "next/link";

const stats = [
  {
    label: "Knowledge Sources",
    description: "Configure and sync data sources",
    icon: Database,
    href: "/dashboard/knowledge",
    color: "text-blue-500",
    bg: "bg-blue-50 dark:bg-blue-950/30",
  },
  {
    label: "Chat Interface",
    description: "Ask questions about your data",
    icon: MessageSquare,
    href: "/dashboard/chat",
    color: "text-emerald-500",
    bg: "bg-emerald-50 dark:bg-emerald-950/30",
  },
];

export default function DashboardPage() {
  return (
    <div className="p-6 max-w-5xl mx-auto space-y-8">
      {/* Hero */}
      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">RAG Platform</h1>
        <p className="text-muted-foreground text-lg">
          Your self-hosted Retrieval-Augmented Generation platform. Configure
          knowledge sources and chat with your data.
        </p>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {stats.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className="group block rounded-xl border bg-card p-6 shadow-sm hover:shadow-md transition-all hover:border-primary/50"
            >
              <div className="flex items-start gap-4">
                <div className={`rounded-lg p-2.5 ${item.bg}`}>
                  <Icon className={`h-6 w-6 ${item.color}`} />
                </div>
                <div className="space-y-1">
                  <h2 className="font-semibold text-lg group-hover:text-primary transition-colors">
                    {item.label}
                  </h2>
                  <p className="text-muted-foreground text-sm">
                    {item.description}
                  </p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      {/* Getting Started */}
      <div className="rounded-xl border bg-card p-6 shadow-sm space-y-4">
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-primary" />
          <h2 className="font-semibold text-lg">Getting Started</h2>
        </div>
        <ol className="space-y-3 text-sm text-muted-foreground list-decimal list-inside">
          <li>
            <Link href="/dashboard/knowledge" className="text-primary hover:underline font-medium">
              Add a knowledge source
            </Link>
            {" "}— configure a website URL, PDF, sitemap, or raw text
          </li>
          <li>
            Click <span className="font-medium text-foreground">Sync</span> to
            fetch, chunk, and embed the content into pgvector
          </li>
          <li>
            Go to{" "}
            <Link href="/dashboard/chat" className="text-primary hover:underline font-medium">
              Chat
            </Link>
            {" "}and start asking questions about your data
          </li>
          <li>
            View traces and analytics in{" "}
            <a
              href="http://localhost:3001"
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary hover:underline font-medium"
            >
              Langfuse
            </a>
          </li>
        </ol>
      </div>

      {/* Links */}
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <BookOpen className="h-5 w-5 text-primary" />
          <h2 className="font-semibold text-lg">Resources</h2>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          {[
            { label: "Backend API", href: "http://localhost:8080/swagger-ui.html" },
            { label: "Langfuse", href: "http://localhost:3001" },
            { label: "PgAdmin", href: "http://localhost:5050" },
            { label: "Health Check", href: "http://localhost:8080/actuator/health" },
          ].map((link) => (
            <a
              key={link.href}
              href={link.href}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-muted-foreground hover:text-primary transition-colors"
            >
              <span className="text-xs">↗</span>
              {link.label}
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
