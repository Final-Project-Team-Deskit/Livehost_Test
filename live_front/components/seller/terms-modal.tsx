"use client"

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { ScrollArea } from "@/components/ui/scroll-area"

interface TermsModalProps {
  open: boolean
  onClose: () => void
}

export function TermsModal({ open, onClose }: TermsModalProps) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>방송 운영 정책</DialogTitle>
        </DialogHeader>
        <ScrollArea className="h-[500px] pr-4">
          <div className="space-y-4 text-sm">
            <section>
              <h3 className="font-semibold mb-2">제1조 목적</h3>
              <p className="text-muted-foreground">
                본 약관은 DESKIT 라이브 커머스 플랫폼(이하 "플랫폼")에서 제공하는 라이브 방송 서비스의 이용과 관련하여
                플랫폼과 판매자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제2조 정의</h3>
              <p className="text-muted-foreground">
                1. "판매자"란 본 약관에 동의하고 플랫폼을 통해 상품을 판매하는 자를 말합니다.
                <br />
                2. "방송"이란 판매자가 플랫폼을 통해 실시간으로 상품을 소개하고 판매하는 행위를 말합니다.
                <br />
                3. "시청자"란 플랫폼을 통해 방송을 시청하는 이용자를 말합니다.
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제3조 방송 운영 기준</h3>
              <p className="text-muted-foreground">
                1. 판매자는 방송 시 다음 각 호의 행위를 하여서는 안 됩니다:
                <br />- 허위 또는 과장된 정보 제공
                <br />- 타인의 권리를 침해하는 내용
                <br />- 음란물, 폭력물 등 유해 콘텐츠
                <br />- 법령을 위반하는 행위
                <br />
                <br />
                2. 판매자는 방송 전 충분한 준비를 하여야 하며, 상품에 대한 정확한 정보를 제공해야 합니다.
                <br />
                3. 판매자는 시청자와의 소통 시 예의를 지켜야 합니다.
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제4조 방송 제재</h3>
              <p className="text-muted-foreground">
                플랫폼은 다음 각 호의 경우 방송을 중단하거나 판매자 자격을 제한할 수 있습니다:
                <br />
                1. 본 약관을 위반한 경우
                <br />
                2. 시청자로부터 다수의 신고가 접수된 경우
                <br />
                3. 법령을 위반한 경우
                <br />
                4. 플랫폼의 운영을 방해하는 경우
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제5조 개인정보 보호</h3>
              <p className="text-muted-foreground">
                판매자는 방송 진행 중 취득한 시청자의 개인정보를 플랫폼의 개인정보처리방침에 따라 안전하게 관리해야
                하며, 서비스 제공 이외의 목적으로 사용하거나 제3자에게 제공해서는 안 됩니다.
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제6조 책임의 한계</h3>
              <p className="text-muted-foreground">
                플랫폼은 판매자가 제공한 정보의 정확성, 신뢰성, 적법성에 대해 책임을 지지 않으며, 판매자와 시청자 간
                발생한 분쟁에 대해 중재 의무를 부담하지 않습니다.
              </p>
            </section>

            <section>
              <h3 className="font-semibold mb-2">제7조 약관의 변경</h3>
              <p className="text-muted-foreground">
                플랫폼은 필요한 경우 본 약관을 변경할 수 있으며, 변경된 약관은 공지사항을 통해 고지합니다. 판매자가
                변경된 약관에 동의하지 않는 경우 서비스 이용을 중단할 수 있습니다.
              </p>
            </section>
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}
