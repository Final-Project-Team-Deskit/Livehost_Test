"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Plus, X, Save, Upload } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { sellerBroadcasts, sellerProducts } from "@/lib/seller-mock-data"
import { ProductPickerModal } from "@/components/seller/product-picker-modal"
import { TermsModal } from "@/components/seller/terms-modal"
import type { Product } from "@/lib/admin-mock-data"
import Image from "next/image"

interface BroadcastProduct extends Product {
  broadcastPrice: number
  quantity: number
}

export default function EditReservationPage({ params }: { params: { id: string } }) {
  const router = useRouter()
  const [step, setStep] = useState(1)
  const broadcast = sellerBroadcasts.find((b) => b.id === params.id)

  // Step 1: Q-card state
  const [qcards, setQcards] = useState([
    { id: "1", question: "오늘의 특별 할인 상품은 무엇인가요?", editing: false },
    { id: "2", question: "배송은 언제 시작되나요?", editing: false },
    { id: "3", question: "반품 정책에 대해 설명해주세요.", editing: false },
  ])

  // Step 2: Basic info state
  const [title, setTitle] = useState(broadcast?.title || "")
  const [category, setCategory] = useState(broadcast?.category || "가구")
  const [notice, setNotice] = useState("판매 상품 외 다른 상품 문의는 받지 않습니다.")
  const [date, setDate] = useState("")
  const [time, setTime] = useState("")
  const [thumbnailPreview, setThumbnailPreview] = useState(broadcast?.thumbnailUrl || "")
  const [waitingPreview, setWaitingPreview] = useState(broadcast?.waitingScreenUrl || "")
  const [productModalOpen, setProductModalOpen] = useState(false)
  const [termsModalOpen, setTermsModalOpen] = useState(false)
  const [termsAgreed, setTermsAgreed] = useState(false)

  const [selectedProducts, setSelectedProducts] = useState<BroadcastProduct[]>(
    sellerProducts.slice(0, 3).map((p) => ({
      ...p,
      broadcastPrice: p.price * 0.9,
      quantity: 10,
    })),
  )

  if (!broadcast) return <div>방송을 찾을 수 없습니다.</div>

  // Q-card handlers
  const handleEditQCard = (id: string) => {
    setQcards(qcards.map((q) => (q.id === id ? { ...q, editing: true } : q)))
  }

  const handleSaveQCard = (id: string, newQuestion: string) => {
    if (!newQuestion.trim()) {
      alert("질문을 입력해주세요.")
      return
    }
    setQcards(qcards.map((q) => (q.id === id ? { ...q, question: newQuestion, editing: false } : q)))
  }

  const handleDeleteQCard = (id: string) => {
    setQcards(qcards.filter((q) => q.id !== id))
  }

  const handleAddQCard = () => {
    if (qcards.length >= 10) {
      alert("큐 카드는 최대 10개까지 등록할 수 있습니다.")
      return
    }
    setQcards([...qcards, { id: Date.now().toString(), question: "", editing: true }])
  }

  const handleNext = () => {
    if (qcards.some((q) => !q.question.trim())) {
      alert("모든 큐 카드 질문을 입력해주세요.")
      return
    }
    setStep(2)
  }

  const handleSave = () => {
    if (!title.trim() || !date || !time || !termsAgreed) {
      alert("모든 필수 항목을 입력하고 약관에 동의해주세요.")
      return
    }
    alert("수정되었습니다.")
    router.push(`/seller/broadcasts/reservations/${params.id}`)
  }

  const updateProductPrice = (productId: string, newPrice: number) => {
    setSelectedProducts((prev) => prev.map((p) => (p.id === productId ? { ...p, broadcastPrice: newPrice } : p)))
  }

  const updateProductQuantity = (productId: string, newQuantity: number) => {
    setSelectedProducts((prev) => prev.map((p) => (p.id === productId ? { ...p, quantity: newQuantity } : p)))
  }

  const removeProduct = (productId: string) => {
    setSelectedProducts((prev) => prev.filter((p) => p.id !== productId))
  }

  const handleThumbnailUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.match(/image\/(jpeg|png)/)) {
      alert("JPG 또는 PNG 형식만 업로드 가능합니다.")
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      alert("썸네일은 최대 5MB까지 업로드 가능합니다.")
      return
    }

    const reader = new FileReader()
    reader.onloadend = () => {
      setThumbnailPreview(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  const handleWaitingScreenUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.match(/image\/(jpeg|png)/)) {
      alert("JPG 또는 PNG 형식만 업로드 가능합니다.")
      return
    }

    if (file.size > 7 * 1024 * 1024) {
      alert("대기화면은 최대 7MB까지 업로드 가능합니다.")
      return
    }

    const reader = new FileReader()
    reader.onloadend = () => {
      setWaitingPreview(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  if (step === 1) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-3xl space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">예약 수정 - 큐 카드 편집</h1>
            <p className="text-sm text-muted-foreground">1/2 단계</p>
          </div>
        </div>

        <Card className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">큐 카드 목록</h3>
            <Button size="sm" onClick={handleAddQCard}>
              <Plus className="h-4 w-4 mr-2" />큐 카드 추가
            </Button>
          </div>

          <div className="space-y-3">
            {qcards.map((qcard, index) => (
              <div key={qcard.id} className="flex items-center gap-2 p-3 border rounded-lg">
                <span className="text-sm font-semibold text-muted-foreground w-8">{index + 1}.</span>
                {qcard.editing ? (
                  <Input
                    defaultValue={qcard.question}
                    onBlur={(e) => handleSaveQCard(qcard.id, e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        handleSaveQCard(qcard.id, e.currentTarget.value)
                      }
                    }}
                    placeholder="질문을 입력하세요"
                    className="flex-1"
                    autoFocus
                  />
                ) : (
                  <p className="flex-1 text-sm">{qcard.question}</p>
                )}
                <div className="flex gap-1">
                  {qcard.editing ? (
                    <Button size="sm" variant="ghost" onClick={() => handleSaveQCard(qcard.id, qcard.question)}>
                      <Save className="h-4 w-4" />
                    </Button>
                  ) : (
                    <Button size="sm" variant="ghost" onClick={() => handleEditQCard(qcard.id)}>
                      수정
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => handleDeleteQCard(qcard.id)}>
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <div className="flex justify-end">
          <Button onClick={handleNext}>다음 단계</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => setStep(1)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">예약 수정 - 기본정보 편집</h1>
          <p className="text-sm text-muted-foreground">2/2 단계</p>
        </div>
      </div>

      <Card className="p-6 space-y-4">
        <div className="space-y-2">
          <label className="text-sm font-semibold">방송 제목 *</label>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="방송 제목을 입력하세요" />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-semibold">카테고리 *</label>
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="가구">가구</SelectItem>
              <SelectItem value="전자기기">전자기기</SelectItem>
              <SelectItem value="패션">패션</SelectItem>
              <SelectItem value="뷰티">뷰티</SelectItem>
              <SelectItem value="악세사리">악세사리</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-semibold">공지사항</label>
          <Textarea value={notice} onChange={(e) => setNotice(e.target.value)} rows={3} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-semibold">날짜 *</label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-semibold">시간 *</label>
            <Select value={time} onValueChange={setTime}>
              <SelectTrigger>
                <SelectValue placeholder="시간 선택" />
              </SelectTrigger>
              <SelectContent>
                {Array.from({ length: 24 }, (_, i) => (
                  <SelectItem key={i} value={`${i.toString().padStart(2, "0")}:00`}>
                    {i.toString().padStart(2, "0")}:00
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-sm font-semibold">판매 상품 선택 *</label>
            <Button variant="outline" size="sm" onClick={() => setProductModalOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              판매상품 불러오기
            </Button>
          </div>

          {selectedProducts.length > 0 && (
            <div className="border rounded-lg overflow-hidden mt-4">
              <table className="w-full text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-2 text-left w-12"></th>
                    <th className="p-2 text-left">상품명</th>
                    <th className="p-2 text-right">실재고</th>
                    <th className="p-2 text-right">판매가</th>
                    <th className="p-2 text-right">방송 할인 가격</th>
                    <th className="p-2 text-right">방송 판매수량</th>
                    <th className="p-2 w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {selectedProducts.map((product) => (
                    <tr key={product.id} className="border-t">
                      <td className="p-2">
                        <div className="w-10 h-10 bg-muted rounded flex-shrink-0 relative overflow-hidden">
                          {product.imageUrl && (
                            <Image
                              src={product.imageUrl || "/placeholder.svg"}
                              alt={product.name}
                              fill
                              className="object-cover"
                            />
                          )}
                        </div>
                      </td>
                      <td className="p-2 font-medium">{product.name}</td>
                      <td className="p-2 text-right">{product.stock}</td>
                      <td className="p-2 text-right">{product.price.toLocaleString()}원</td>
                      <td className="p-2">
                        <Input
                          type="number"
                          value={product.broadcastPrice}
                          onChange={(e) => updateProductPrice(product.id, Number(e.target.value))}
                          className="w-28 text-right"
                          min="0"
                        />
                      </td>
                      <td className="p-2">
                        <Input
                          type="number"
                          value={product.quantity}
                          onChange={(e) => updateProductQuantity(product.id, Number(e.target.value))}
                          className="w-20 text-right"
                          min="1"
                          max={product.stock}
                        />
                      </td>
                      <td className="p-2">
                        <Button variant="ghost" size="icon" onClick={() => removeProduct(product.id)}>
                          <X className="h-4 w-4" />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="text-xs text-muted-foreground">{selectedProducts.length}/10 개 선택됨 (최소 1개, 최대 10개)</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-semibold">썸네일 업로드 *</label>
            <label className="block cursor-pointer">
              <input type="file" accept="image/jpeg,image/png" className="hidden" onChange={handleThumbnailUpload} />
              <div className="border-2 border-dashed rounded-lg p-4 text-center hover:border-primary transition-colors aspect-[9/16] flex items-center justify-center">
                {thumbnailPreview ? (
                  <div className="relative w-full h-full">
                    <Image
                      src={thumbnailPreview || "/placeholder.svg"}
                      alt="썸네일"
                      fill
                      className="object-cover rounded"
                    />
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-2">
                    <Upload className="h-8 w-8 text-muted-foreground" />
                    <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                    <p className="text-xs text-muted-foreground">JPG/PNG, 최대 5MB</p>
                  </div>
                )}
              </div>
            </label>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-semibold">대기화면 업로드 *</label>
            <label className="block cursor-pointer">
              <input
                type="file"
                accept="image/jpeg,image/png"
                className="hidden"
                onChange={handleWaitingScreenUpload}
              />
              <div className="border-2 border-dashed rounded-lg p-4 text-center hover:border-primary transition-colors aspect-[9/16] flex items-center justify-center">
                {waitingPreview ? (
                  <div className="relative w-full h-full">
                    <Image
                      src={waitingPreview || "/placeholder.svg"}
                      alt="대기화면"
                      fill
                      className="object-cover rounded"
                    />
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-2">
                    <Upload className="h-8 w-8 text-muted-foreground" />
                    <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                    <p className="text-xs text-muted-foreground">JPG/PNG, 최대 7MB</p>
                  </div>
                )}
              </div>
            </label>
          </div>
        </div>

        <div className="flex items-start gap-2 p-4 bg-muted rounded-lg">
          <Checkbox checked={termsAgreed} onCheckedChange={(checked) => setTermsAgreed(checked === true)} />
          <div className="flex-1">
            <p className="text-sm">
              방송 운영 정책 및 약관에 동의합니다.{" "}
              <button type="button" onClick={() => setTermsModalOpen(true)} className="text-primary underline">
                자세히 보기
              </button>
            </p>
          </div>
        </div>
      </Card>

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          취소
        </Button>
        <Button onClick={handleSave} disabled={!termsAgreed}>
          저장
        </Button>
      </div>

      <ProductPickerModal
        open={productModalOpen}
        onClose={() => setProductModalOpen(false)}
        onSave={(products) =>
          setSelectedProducts(
            products.map((p) => ({
              ...p,
              broadcastPrice: p.price,
              quantity: 1,
            })),
          )
        }
        currentlySelected={selectedProducts}
      />
      <TermsModal open={termsModalOpen} onClose={() => setTermsModalOpen(false)} />
    </div>
  )
}
