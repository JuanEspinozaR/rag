import { ChatSidebar } from "@/components/chat/ChatSidebar";

export default function ChatLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex h-full overflow-hidden">
      <ChatSidebar />
      <div className="flex-1 overflow-hidden">{children}</div>
    </div>
  );
}
